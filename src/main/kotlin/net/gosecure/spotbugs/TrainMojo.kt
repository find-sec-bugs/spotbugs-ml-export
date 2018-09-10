package net.gosecure.spotbugs

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import weka.classifiers.*
import net.gosecure.spotbugs.datasource.ml.MLUtils
import weka.classifiers.bayes.NaiveBayes

@Mojo(name="train")
class TrainMojo: AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")

    private lateinit var project: MavenProject

    private val FILE_INPUT = "spotbugs-results.csv"
    private val FILE_OUTPUT = "spotbugs-results.arff"
    private val MODEL_SAVED = "spotbugs-results-ml.model"

    override fun execute() {
        log.info("Training...")

        //Instantiate configuration
        val cfg = MLUtils().initConfig()

        val dataUnfiltered = MLUtils().getInstances(project, FILE_INPUT, FILE_OUTPUT)
        val dataFiltered = MLUtils().filterMeta(dataUnfiltered)
        dataFiltered.setClassIndex(dataFiltered.numAttributes() - 1)

        // Use a set of classifiers
        val models = arrayOf<Classifier>(NaiveBayes())

        // Run for each model
        for (j in models.indices) {
            System.out.println("\n" + models[j].javaClass.simpleName)

            //10 fold-cross validation, print stats data in html
            MLUtils().trainStats(project, cfg, models[j], dataFiltered)

            //Train on full data : build the classifier
            MLUtils().trainFullData(models[j], dataFiltered)

            MLUtils().saveModel(project, models[j], MODEL_SAVED)
        }
    }

}