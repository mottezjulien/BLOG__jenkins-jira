#!groovy

def param_branchName = params.BRANCH_NAME
def param_mavenBuild = params.MAVEN_BUILD

def gitCredentialsId = 'aaa-bbb-ccc'
def gitRepoUrl = 'https://jenkins-user@repogit.com/xxxxx/yyyy.git'

node {

    step([$class: 'WsCleanup'])

    git branch: param_branchName, credentialsId: gitCredentialsId, url: gitRepoUrl
    try {
        sh "mvn " + param_mavenBuild
        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
    } catch(err){
        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
        if (currentBuild.result == 'UNSTABLE') {
            currentBuild.result = 'FAILURE'
        }
        throw err
    }
}