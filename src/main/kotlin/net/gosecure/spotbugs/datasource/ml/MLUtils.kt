package net.gosecure.spotbugs.datasource.ml

import freemarker.template.Configuration
import freemarker.template.TemplateException
import freemarker.template.TemplateExceptionHandler
import org.apache.maven.project.MavenProject
import weka.classifiers.Classifier
import weka.classifiers.Evaluation
import weka.core.Instances
import weka.core.converters.ArffSaver
import weka.core.converters.CSVLoader
import weka.filters.Filter
import weka.filters.unsupervised.attribute.Add
import weka.filters.unsupervised.attribute.Remove
import java.io.*
import java.util.*

class MLUtils {

    fun getResourcePath(project: MavenProject, fileName: String): String {

        val f = File(project.build.directory,"/spotbugs-ml")
        return f.absolutePath + File.separator + fileName
    }

    fun getResource(project: MavenProject, fileName: String): File {

        val completeFileName = getResourcePath(project, fileName)
        return File(completeFileName)
    }


    @Throws(IOException::class)
    fun csvToArff(csv: File, arff: File) {
        // load CSV
        val loader = CSVLoader()
        loader.setSource(csv)
        val data = loader.getDataSet()

        // save ARFF
        val saver = ArffSaver()
        saver.setInstances(data)
        saver.setFile(arff)
        saver.writeBatch()
    }

    fun getInstances(project: MavenProject, csv: String, arff: String): Instances {
        val input = getResource(project, csv)
        val output = getResource(project, arff)
        csvToArff(input, output)

        val datafile = BufferedReader(FileReader(output))

        return Instances(datafile)
    }

    fun filterMeta(data: Instances): Instances {
        var data = data
        //Filter metadatas (here SourceFile, Line, GroupId, ArtifactId, Author, key)

        val options = arrayOf("-R", "1,2,3,4,14")
        val remove = Remove()
        remove.setOptions(options)
        remove.setInputFormat(data)
        data = Filter.useFilter(data, remove)

        return data
    }

    @Throws(Exception::class)
    fun createClassAttribute(data: Instances): Instances {
        var data = data
        val remove = Remove()
        remove.setAttributeIndicesArray(intArrayOf(data.numAttributes() - 1))
        remove.setInputFormat(data)
        data = Filter.useFilter(data, remove)

        val add = Add()
        add.setAttributeIndex("last")
        add.setNominalLabels("BAD,GOOD")
        add.setAttributeName("Status")
        add.setInputFormat(data)
        data = Filter.useFilter(data, add)

        return data
    }

    @Throws(Exception::class)
    fun loadModel(project: MavenProject, nameModel: String): Classifier {
        var name = project.build.directory + "/spotbugs-ml/" + nameModel
        return weka.core.SerializationHelper.read(name) as Classifier
    }

    @Throws(Exception::class)
    fun saveModel(project: MavenProject, model: Classifier, nameModel: String) {
        var name = project.build.directory + "/spotbugs-ml/" + nameModel
        weka.core.SerializationHelper.write(name, model)
    }


    @Throws(Exception::class)
    fun trainStats(project: MavenProject, cfg: Configuration, model: Classifier, data: Instances) {
        val eval = Evaluation(data)
        eval.crossValidateModel(model, data, 10, Random(1))

        val recall = java.lang.Double.toString(eval.recall(0))
        val precision = java.lang.Double.toString(eval.precision(0))
        val fmeasure = java.lang.Double.toString(eval.fMeasure(0))
        val accuracy = java.lang.Double.toString(eval.pctCorrect())
        val confusionMatrix = eval.confusionMatrix()
        val confusionMatrixString = eval.toMatrixString()

        println("Estimated Recal : $recall")
        println("Estimated Precision : $precision")
        println("Estimated F-measure : $fmeasure")
        println("Estimated Accuracy : $accuracy")
        println("Confusion Matrix : $confusionMatrixString")

        outputHtmlTrain(project, cfg, recall, precision, fmeasure, accuracy, confusionMatrix)
    }

    @Throws(Exception::class)
    fun trainFullData(model: Classifier, data: Instances): Classifier {
        model.buildClassifier(data)
        return model
    }

    // After training, make predictions on instances, and print the prediction and real values
    @Throws(Exception::class)
    fun makePredictions(project: MavenProject, cfg: Configuration, unfiltered: Instances, unlabeled: Instances, model: Classifier) {

        val labeled = Instances(unlabeled)

        val issues = ArrayList<Issue>()
        var number = 0
        for (i in 0 until (unlabeled.numInstances() - 1)) {

            val newInst = unlabeled.instance(i)

            val predNb = model.classifyInstance(newInst)
            labeled.instance(i).setClassValue(predNb)

            val predString = labeled.classAttribute().value(predNb.toInt())
            val pred = model.distributionForInstance(labeled.get(i))

            //Instances classified with a probability < 90%
            if (Math.max(pred[0], pred[1]) < 0.9) {
                val sourceFile = unfiltered.instance(i).stringValue(0) //Source File Attribute 1
                val line = Integer.toString(unfiltered.instance(i).value(1).toInt()) //Line Attribute 2
                val bugType = unfiltered.instance(i).stringValue(5) //BugType Attribute 5
                issues.add(Issue(sourceFile, line, bugType))
                number++
            }
        }
        outputHtmlPredict(project, cfg, issues, number)
    }

    @Throws(IOException::class)
    fun initConfig(): Configuration {
        val cfg = Configuration()
        cfg.setClassForTemplateLoading(this.javaClass, "/net/gosecure/spotbugs/")
        cfg.setDefaultEncoding("UTF-8")
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
        return cfg
    }

    @Throws(IOException::class, TemplateException::class)
    fun outputHtmlTrain(project: MavenProject, cfg: Configuration, recall: String, precision: String, fmeasure: String, accuracy: String, confusionMatrix: Array<DoubleArray>) {

        //Data model
        val map = HashMap<String, Any>()
        map.put("recall", recall)
        map.put("precision", precision)
        map.put("fmeasure", fmeasure)
        map.put("accuracy", accuracy)
        map.put("badbad", confusionMatrix[0][0])
        map.put("badgood", confusionMatrix[0][1])
        map.put("goodbad", confusionMatrix[1][0])
        map.put("goodgood", confusionMatrix[1][1])

        //Instantiate template
        val template = cfg.getTemplate("training-results.ftl")

        //Console output
        val console = OutputStreamWriter(System.out)
        template.process(map, console)
        console.flush()

        // File output
        val file = FileWriter(File(project.build.directory + "/spotbugs-ml", "training-results.html"))
        template.process(map, file)
        file.flush()
        file.close()
    }

    @Throws(IOException::class, TemplateException::class)
    fun outputHtmlPredict(project: MavenProject, cfg: Configuration, issues: List<Issue>, number: Int) {

        //Data model
        val map = HashMap<String, Any>()
        map.put("numberIssues", number.toString())
        map.put("issues", issues)

        //Instantiate template
        val template = cfg.getTemplate("predictions-results.ftl")

        //Console output
        val console = OutputStreamWriter(System.out)
        template.process(map, console)
        console.flush()

        // File output
        val file = FileWriter(File(project.build.directory + "/spotbugs-ml","predictions-results.html"))
        template.process(map, file)
        file.flush()
        file.close()
    }
}