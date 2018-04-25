//Importar librerÃ­as
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext,Row}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.functions._
import org.apache.spark.mllib.linalg.{DenseVector,Vector}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.ml.feature.{Normalizer,StandardScaler}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.apache.spark.ml.feature.PCA
import java.util.Calendar
import org.apache.hadoop.fs._;





//Leer datos y ajustar para su procesamiento
case class Obs( id: Float, v17: Float, v19: Float, 
	v20: Float, v23: Float, v24: Float, v26: Float, v29: Float)

def parseObs(line: Array[Float]): Obs = {
    Obs( line(0), line(1), line(2), line(3), line(4), line(5), line(6), line(7)  )     }
	
def parseRDD(rdd: RDD[String]): RDD[Array[Float]] = {
    rdd.map(_.split(",")).map(_.drop(0)).map(_.map(_.toFloat))}

val rdd = sc.textFile("/home/centos/DataIPS/table1.txt")
val obsRDD = parseRDD(rdd).map(parseObs)
val obsDF = obsRDD.toDF().cache()
obsDF.registerTempTable("obs")



val featureCols = Array("v17","v19","v24","v29")
val assembler = new VectorAssembler().setInputCols(featureCols).setOutputCol("featuresPCA")
val df2 = assembler.transform(obsDF)

val pca = new PCA().setInputCol("featuresPCA").
  setOutputCol("features").
  setK(2).
  fit(df2)
  

val result = pca.transform(df2).select("features")



import org.apache.spark.ml.clustering.KMeans

/*
val besterror  	  = new ArrayBuffer[Double]()
val numcluster    = new ArrayBuffer[Double]()



besterror += (1e100)
// Trains a k-means model.
for(i<-  2 to 20)
{
	val kmeans = new KMeans().setK(i)
		val model = kmeans.fit(result)
	
	// Evaluate clustering by computing Within Set Sum of Squared Errors.
	val WSSSE = model.computeCost(result)
	
	if (WSSSE < besterror.last)
	{
		besterror += WSSSE
		numcluster += i 		
	}

}
 */	
val kmeans = new KMeans().setK(5).setSeed(17962007).setMaxIter(100)
val model2 = kmeans.fit(result)

/* result.show

// Evaluate clustering by computing Within Set Sum of Squared Errors.
val WSSSE = model.computeCost(result)
println(s"Within Set Sum of Squared Errors = $WSSSE")

// Shows the result.
println("Cluster Centers: ")
model.clusterCenters.foreach(println)
model.summary.predictions.show
 */



/*GaussianMixture Model
import org.apache.spark.ml.clustering.GaussianMixture

 val besterror2  	  = new ArrayBuffer[Double]()
val numcluster2    = new ArrayBuffer[Int]()
val error2     	= new ArrayBuffer[Double]()

besterror2 += (0)
for(i<-  2 to 20)
{
	// Trains Gaussian Mixture Model
	val gmm = new GaussianMixture().setK(i)
	val model = gmm.fit(result)
	error2 += model.summary.logLikelihood
	
	if (model.summary.logLikelihood> besterror2.last)
	{
		besterror2  += model.summary.logLikelihood
		numcluster2 += i 		
	}
}
 
 
 
// Trains Gaussian Mixture Model
val gmm = new GaussianMixture().setK(5).setSeed(1947).setMaxIter(100)

val model2 = gmm.fit(result)
*/	
//Separar valores de PCA
val p = result.takeAsList(40).toString.replace("[","").replace("]","").split(",")
val dou = p.map(x => x.toDouble)
val par = new ArrayBuffer[Double]()
val impar = new ArrayBuffer[Double]()

for (i <- 0 until 40) 
{
  if (i%2 ==0) par += dou(i) else impar += dou(i)
}
val pca1 = par.toDF("pca1")
val pca11 = pca1.withColumn("id", monotonicallyIncreasingId+1.toFloat	)

val pca2 = impar.toDF("pca2")
val pca22 = pca2.withColumn("id", monotonicallyIncreasingId+1.toFloat	)

val pca33=  pca11.join(pca22,"id")

//calcular distancia por ips
val distance = new ArrayBuffer[Double]()
for (i <- 0 until 20) 
{
   distance += math.sqrt(math.pow(par(i),2)+math.pow(impar(i),2))

}

//Añadir la distancia y los valores de PCA al dataframe
val df4 = distance.map(x => x.toDouble).toDF("distance")
val dis = df4.withColumn("id", monotonicallyIncreasingId+1.toFloat	)

val df5 = df2.drop(df2.col("featuresPCA")).join(dis,"id")
val df6 = model2.summary.cluster
val df66 = spark.sqlContext.createDataFrame(
    df6.rdd.zipWithIndex.map {
    case (row, index) => Row.fromSeq(row.toSeq :+ index.toFloat+ 1)
    },
    // Create schema for index column
    StructType(df6.schema.fields :+ StructField("id", FloatType, true)))
val df7 = df5.join(df66,"id")
val df8=df7.join(pca33,"id")


//Export table data graph
val df= spark.read.format("csv").option("encoding","ISO-8859-1").load("/home/centos/DataIPS/names.txt").withColumnRenamed("_c0","name")
val df23 = df.withColumn("id", monotonicallyIncreasingId+1.toFloat	)
val df19 = df23.join(df8,"id")

val df20 = df19.select("id","distance","prediction").groupBy("prediction").avg().select("avg(distance)","prediction")
val df21 = df19.join(df20,"prediction").orderBy("avg(distance)")


//Export scatter data graph
val cxs = Vector(model2.clusterCenters(0)(0),model2.clusterCenters(1)(0),model2.clusterCenters(2)(0),model2.clusterCenters(3)(0),model2.clusterCenters(4)(0)).toDF("cx").withColumn("id",monotonicallyIncreasingId)

val cms=  Vector(model2.summary.clusterSizes(0),model2.summary.clusterSizes(1),model2.summary.clusterSizes(2),model2.summary.clusterSizes(3),model2.summary.clusterSizes(4)).toDF("cm").withColumn("id",monotonicallyIncreasingId)

val m0 =df21.filter("prediction == 0").select("name").collect.toBuffer.toString
val m1 =df21.filter("prediction == 1").select("name").collect.toBuffer.toString
val m2 =df21.filter("prediction == 2").select("name").collect.toBuffer.toString
val m3 =df21.filter("prediction == 3").select("name").collect.toBuffer.toString
val m4 =df21.filter("prediction == 4").select("name").collect.toBuffer.toString

val ms =Vector(m0,m1,m2,m3,m4).toDF("m").withColumn("id",monotonicallyIncreasingId)

val scatter = cxs.join(cms,"id").join(ms,"id").drop("id")

//df21.filter("prediction == 0").select("name").withColumn("id", monotonicallyIncreasingId+1.toFloat	)

//val pca44 = pca33.withColumnRenamed("pca1","x").withColumnRenamed("pca2","y").join(df23,"id").drop("id")
scatter.coalesce(1).write.mode("overwrite").json("/var/www/html/DashBoardIPS/web/scatter/")
val fs = FileSystem.get(sc.hadoopConfiguration);
val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/scatter/part*"))(0).getPath().getName();
fs.rename(new Path("/var/www/html/DashBoardIPS/web/scatter/"+file), new Path("/var/www/html/DashBoardIPS/web/scatter/scatter.json"));


//Export data backup
	
	//Rad file name
	val lines = scala.io.Source.fromFile("/home/centos/DataIPS/date", "utf-8").getLines.mkString
	val x = lines.replace(":","-")

df21.orderBy("avg(distance)").coalesce(1).write.mode("overwrite").json("/home/centos/DataIPS/backup/"++x)
val fs = FileSystem.get(sc.hadoopConfiguration);
val file = fs.globStatus(new Path("/home/centos/DataIPS/backup/"++x++"/part*"))(0).getPath().getName();
fs.rename(new Path("/home/centos/DataIPS/backup/"++x++"/"+file), new Path("/home/centos/DataIPS/backup/"++x++"/"+"data.json"));


//Export data tables
val cluslist = df21.orderBy("avg(distance)").select("prediction").collect()
val xx =  cluslist(0).toString.replace("[","").replace("]","").toInt
val yy =  cluslist(19).toString.replace("[","").replace("]","").toInt

val bestcluster  = df21.filter("prediction =="+xx)
val worstcluster = df21.filter("prediction =="+yy)
	//the betters
    bestcluster.select("name").coalesce(1).write.mode("overwrite").json("/var/www/html/DashBoardIPS/web/tablebest/")
	val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/tablebest/part*"))(0).getPath().getName();
	fs.rename(new Path("/var/www/html/DashBoardIPS/web/tablebest/"+file), new Path("/var/www/html/DashBoardIPS/web/tablebest/table.json"));
	
	//the worsts
    worstcluster.select("name").coalesce(1).write.mode("overwrite").json("/var/www/html/DashBoardIPS/web/tableworst/")
	val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/tableworst/part*"))(0).getPath().getName();
	fs.rename(new Path("/var/www/html/DashBoardIPS/web/tableworst/"+file), new Path("/var/www/html/DashBoardIPS/web/tableworst/table.json"));
 
//Export data donut  
	//donut 1
	val df25 = Vector(model2.summary.clusterSizes(xx)).toDF("cluster")
	val df27 = df25.withColumn("total",lit(20))
	df27.coalesce(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header", "FALSE").save("/var/www/html/DashBoardIPS/web/donut1/")
	val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/donut1/part*"))(0).getPath().getName();
	fs.rename(new Path("/var/www/html/DashBoardIPS/web/donut1/"+file), new Path("/var/www/html/DashBoardIPS/web/donut1/donut.json"));
	// donut 2
	val df26 = Vector(model2.summary.clusterSizes(yy)).toDF("cluster")
	val df28 = df26.withColumn("total",lit(20))
	df28.coalesce(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header", "FALSE").save("/var/www/html/DashBoardIPS/web/donut2/")
	val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/donut2/part*"))(0).getPath().getName();
	fs.rename(new Path("/var/www/html/DashBoardIPS/web/donut2/"+file), new Path("/var/www/html/DashBoardIPS/web/donut2/donut.json"));
	
	//Velas

	//V17
	val a17 = df2.describe("v17").select("v17").collect()
	val mean17 = a17(1).toString.replace("[","").replace("]","").toDouble
	val stdv17 = a17(2).toString.replace("[","").replace("]","").toDouble

	val df171 = Vector(mean17-stdv17).toDF("close").withColumn("id", monotonicallyIncreasingId+1.toFloat	)
	val df172 = Vector(mean17+stdv17).toDF("open").withColumn("id", monotonicallyIncreasingId+1.toFloat	)
	val df173 = Vector(mean17).toDF("mean").withColumn("id", monotonicallyIncreasingId+1.toFloat	)
	val df175 = Vector("V17").toDF("name").withColumn("id", monotonicallyIncreasingId+1.toFloat	)

 
	//V19
	val a19 = df2.describe("v19").select("v19").collect()
	val mean19 = a19(1).toString.replace("[","").replace("]","").toDouble
	val stdv19 = a19(2).toString.replace("[","").replace("]","").toDouble

	
	val df191 = Vector(mean19-stdv19).toDF("close").withColumn("id", monotonicallyIncreasingId+2.toFloat	)
	val df192 = Vector(mean19+stdv19).toDF("open").withColumn("id", monotonicallyIncreasingId+2.toFloat	)
	val df193 = Vector(mean19).toDF("mean").withColumn("id", monotonicallyIncreasingId+2.toFloat	)
	val df195 = Vector("V19").toDF("name").withColumn("id", monotonicallyIncreasingId+2.toFloat	)
 
	//V20
	val a20 = df2.describe("v20").select("v20").collect()
	val mean20 = a20(1).toString.replace("[","").replace("]","").toDouble
	val stdv20 = a20(2).toString.replace("[","").replace("]","").toDouble
	val min20 = a20(3).toString.replace("[","").replace("]","").toDouble
	val max20 = a20(4).toString.replace("[","").replace("]","").toDouble

	val df201 = Vector(mean20-stdv20).toDF("close").withColumn("id", monotonicallyIncreasingId+3.toFloat	)
	val df202 = Vector(mean20+stdv20).toDF("open").withColumn("id", monotonicallyIncreasingId+3.toFloat	)
	val df203 = Vector(mean20).toDF("mean").withColumn("id", monotonicallyIncreasingId+3.toFloat	)
	val df205 = Vector("V20").toDF("name").withColumn("id", monotonicallyIncreasingId+3.toFloat	)
	
	//V23
	val a23 = df2.describe("v23").select("v23").collect()
	val mean23 = a23(1).toString.replace("[","").replace("]","").toDouble
	val stdv23 = a23(2).toString.replace("[","").replace("]","").toDouble
	val min23 = a23(3).toString.replace("[","").replace("]","").toDouble
	val max23 = a23(4).toString.replace("[","").replace("]","").toDouble
	
	val df231 = Vector(mean23-stdv23).toDF("close").withColumn("id", monotonicallyIncreasingId+4.toFloat	)
	val df232 = Vector(mean23+stdv23).toDF("open").withColumn("id", monotonicallyIncreasingId+4.toFloat	)
	val df233 = Vector(mean23).toDF("mean").withColumn("id", monotonicallyIncreasingId+4.toFloat	)
	val df235 = Vector("V23").toDF("name").withColumn("id", monotonicallyIncreasingId+4.toFloat	) 

	//V24
	val a24 = df2.describe("v24").select("v24").collect()
	val mean24 = a24(1).toString.replace("[","").replace("]","").toDouble
	val stdv24 = a24(2).toString.replace("[","").replace("]","").toDouble
	val min24 = a24(3).toString.replace("[","").replace("]","").toDouble
	val max24 = a24(4).toString.replace("[","").replace("]","").toDouble


	val df241 = Vector(mean24-stdv24).toDF("close").withColumn("id", monotonicallyIncreasingId+5.toFloat	)
	val df242 = Vector(mean24+stdv24).toDF("open").withColumn("id", monotonicallyIncreasingId+5.toFloat	)
	val df243 = Vector(mean24).toDF("mean").withColumn("id", monotonicallyIncreasingId+5.toFloat	)
	val df245 = Vector("V24").toDF("name").withColumn("id", monotonicallyIncreasingId+5.toFloat	)
	
	//V26
	val a26 = df2.describe("v26").select("v26").collect()
	val mean26 = a26(1).toString.replace("[","").replace("]","").toDouble
	val stdv26 = a26(2).toString.replace("[","").replace("]","").toDouble
	val min26 = a26(3).toString.replace("[","").replace("]","").toDouble
	val max26 = a26(4).toString.replace("[","").replace("]","").toDouble

	
	val df261 = Vector(mean26-stdv26).toDF("close").withColumn("id", monotonicallyIncreasingId+6.toFloat	)
	val df262 = Vector(mean26+stdv26).toDF("open").withColumn("id", monotonicallyIncreasingId+6.toFloat	)
	val df263 = Vector(mean26).toDF("mean").withColumn("id", monotonicallyIncreasingId+6.toFloat	)
	val df265 = Vector("V26").toDF("name").withColumn("id", monotonicallyIncreasingId+6.toFloat	)
	
	//V29
	val a29 = df2.describe("v29").select("v29").collect()
	val mean29 = a29(1).toString.replace("[","").replace("]","").toDouble
	val stdv29 = a29(2).toString.replace("[","").replace("]","").toDouble
	val min29 = a29(3).toString.replace("[","").replace("]","").toDouble
	val max29 = a29(4).toString.replace("[","").replace("]","").toDouble

	val df291 = Vector(mean29-stdv29).toDF("close").withColumn("id", monotonicallyIncreasingId+7.toFloat	)
	val df292 = Vector(mean29+stdv29).toDF("open").withColumn("id", monotonicallyIncreasingId+7.toFloat	)
	val df293 = Vector(mean29).toDF("mean").withColumn("id", monotonicallyIncreasingId+7.toFloat	)
	val df295 = Vector("V29").toDF("name").withColumn("id", monotonicallyIncreasingId+7.toFloat	)
	

  val df267 = df265.join(df262,"id").join(df263,"id").join(df261,"id")
  val df247 = df245.join(df242,"id").join(df243,"id").join(df241,"id")
  val df237 = df235.join(df232,"id").join(df233,"id").join(df231,"id")
  val df207 = df205.join(df202,"id").join(df203,"id").join(df201,"id")
  val df197 = df195.join(df192,"id").join(df193,"id").join(df191,"id")
  val df297 = df295.join(df292,"id").join(df293,"id").join(df291,"id")
  val df177 = df175.join(df172,"id").join(df173,"id").join(df171,"id")

  val df300 = df177.union(df197).union(df207).union(df237).union(df247).union(df267).union(df297) 
	df300.drop("id").coalesce(1).write.mode("overwrite").json("/var/www/html/DashBoardIPS/web/bars-whisker/")
	val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/bars-whisker/part*"))(0).getPath().getName();
	fs.rename(new Path("/var/www/html/DashBoardIPS/web/bars-whisker/"+file), new Path("/var/www/html/DashBoardIPS/web/bars-whisker/table.json"));
 

//Export table data graph
val df111 =  spark.read.format("csv").option("encoding","ISO-8859-1").load("/home/centos/DataIPS/variables.txt").withColumnRenamed("_c0","var").withColumnRenamed("_c1","name")
df111.coalesce(1).write.mode("overwrite").json("/var/www/html/DashBoardIPS/web/name-var/")
val file = fs.globStatus(new Path("/var/www/html/DashBoardIPS/web/name-var/part*"))(0).getPath().getName();
fs.rename(new Path("/var/www/html/DashBoardIPS/web/name-var/"+file), new Path("/var/www/html/DashBoardIPS/web/name-var/name.json"));
System.exit(0) 
