package net.gosecure.spotbugs

import net.gosecure.spotbugs.datasource.MLUtils
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import weka.classifiers.Classifier
import weka.classifiers.bayes.NaiveBayes

@Mojo(name="predict")
class PredictMojo : AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")

    private lateinit var project: MavenProject

    private val FILE_INPUT = "aggregate-results_classified_sample.csv"
    private val FILE_OUTPUT = "aggregate-results_classified_sample.arff"
    private val FILE_INPUT_TEST = "aggregate-results_classified_sample_unlabeled.csv"
    private val FILE_OUTPUT_TEST = "aggregate-results_classified_sample_unlabeled.arff"
    private val MODEL_SAVED = "test-saved-model.model"

    override fun execute() {
        log.info("Predicting...")

        //Instantiate configuration
        val cfg = MLUtils().initConfig()

        val dataUnfiltered = MLUtils().getInstances(project, FILE_INPUT, FILE_OUTPUT)

        val model = MLUtils().loadModel(project, MODEL_SAVED)

        var dataTestTemp = MLUtils().getInstances(project, FILE_INPUT_TEST, FILE_OUTPUT_TEST)
        dataTestTemp = MLUtils().filterMeta(dataTestTemp)
        val dataUnlabeled = MLUtils().createClassAttribute(dataTestTemp)

        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1)

        MLUtils().makePredictions(project, cfg, dataUnfiltered, dataUnlabeled, model)
    }
}