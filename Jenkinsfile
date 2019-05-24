@Library('pipeline-lib') _
pipeline {
    agent any

    parameters {
        string(name: 'yamlFile', defaultValue: 'app-preprod.yaml', description: 'Yaml fil som applies')
    }


    environment {
        APPLICATION_NAME = 'sak'
        APPLICATION_VERSION = version()
    }
    tools {
        maven "maven-3.6.1"
        jdk "8"
    }

    stages {
        stage('Maven Build') {
            steps {
                script {
                    sh "mvn clean install -Drevision=${env.APPLICATION_VERSION}"
                }
            }
        }

        stage('Build and push docker image') {
            steps {
                script {
                    sh "docker build -t repo.adeo.no:5443/${env.APPLICATION_NAME}:${env.APPLICATION_VERSION} --pull ."
                    sh "docker push repo.adeo.no:5443/${env.APPLICATION_NAME}:${env.APPLICATION_VERSION}"
                }

            }
        }

        stage('Update yaml file') {
            steps {
                script {
                    def yaml = "${params.yamlFile}"
                    echo "klar for å lese yamlfile $yaml"
                    def yamlFile = readFile(yaml).replaceAll("@@version@@", "${env.APPLICATION_VERSION}")
                    echo "Klar for å skrive til fil"
                    writeFile file: yaml, text: yamlFile
                }
            }
        }

        stage('Deploy to preprod') {
            steps {
                script {
                    sh "kubectl --context preprod-fss apply -f ${params.yamlFile}"
                }
            }
        }
    }
}
