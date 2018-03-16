#!groovy

def gitCredentialsId = 'aaa-bbb-ccc'
def gitRepoUrl = 'https://jenkins-user@repogit.com/xxxxx/yyyy.git'
def gitBranchMaster = 'master'

def jiraJqlCurrentMergingPending = '(NOT issuetype = Sous-tâche) AND sprint in openSprints () AND status = "Merge pending"'

def git-branch-and-maven-build_jobName = 'generic-fct-git-branch-and-maven-build'
def git-branch-and-maven-build_branchNameJobParam = 'BRANCH_NAME'
def git-branch-and-maven-build_mavenBuildJobParam = 'MAVEN_BUILD'

def mavenBuild = 'clean install -P integrationTests'

def jiraIssues = [];
def remoteBranches = [];

def mergingJiraIssues = [];
def mergingJiraIssuesWithGitBranches = [:]
def mergingJiraIssuesSuccessResults = [:]

def mailTo ='team@mycompany.com'
def successResult = true

node {

    stage('Find MergingPending Issues') {
        jiraIssues = jiraIssueSelector(issueSelector: [$class: 'JqlIssueSelector', jql: jiraJqlCurrentMergingPending])
    }

    stage('Find all git branches') {
        git branch: gitBranchMaster, credentialsId: gitCredentialsId, url: gitRepoUrl
        String remoteBranchesStr = sh (
                script: "git branch -r",
                returnStdout: true
        ).trim()
        remoteBranchesStr = remoteBranchesStr.replaceAll("\n", "")
        remoteBranchesStr = remoteBranchesStr.replaceAll("\r", "")
        remoteBranchesStr = remoteBranchesStr.replaceAll(" ", "")

        remoteBranches = remoteBranchesStr.split('origin/');
    }

    stage('find link between Jira Issue and Git Branch') {
        for (jiraIssue in jiraIssues) {
            for (remoteBranch in remoteBranches) {
                if(remoteBranch.startsWith(jiraIssue)) {
                    echo "link Git branch " + remoteBranch + " with Jira Issue " + jiraIssue
                    mergingJiraIssues.add(jiraIssue)
                    mergingJiraIssuesWithGitBranches.put(jiraIssue, remoteBranch)
                }
            }
        }
    }

    for(int i = 0; i < mergingJiraIssues.size(); i++) {
        def jiraIssue = mergingJiraIssues.get(i)
        def gitBranch = mergingJiraIssuesWithGitBranches.get(jiraIssue)
        stage(jiraIssue + ':Execute ' + git-branch-and-maven-build_jobName + ' with the Jira issue ' + jiraIssue) {
            try {
                build job: git-branch-and-maven-build_jobName, parameters:
                        [[$class: 'StringParameterValue', name: git-branch-and-maven-build_branchNameJobParam, value: gitBranch],
                         [$class: 'StringParameterValue', name: git-branch-and-maven-build_mavenBuildJobParam, value: mavenBuild]]
                echo jiraIssue + ':success !!'
                mergingJiraIssuesSuccessResults.put(jiraIssue, true)
                jiraComment body: "Jenkins Message: issue validated - merge permitted  " + env.JOB_NAME + " -> " + env.BUILD_NUMBER, issueKey: jiraIssue
            } catch (err) {
                echo jiraIssue + ':exception !!'
                mergingJiraIssuesSuccessResults.put(jiraIssue, false)
                successResult = false
            }
        }
    }

    stage('Report') {
        if(mergingJiraIssuesSuccessResults.size() > 0) {
            def resultText = "Success"
            if(!successResult){
                currentBuild.result = 'FAILURE'
                resultText = "Failure"
            }

            def subjectMail = "Jenkins: Job " + env.JOB_NAME + " [" + env.BUILD_NUMBER + "] - " + resultText
            def body = "<p>Build ${resultText}: Job " + env.JOB_NAME + " [" + env.BUILD_NUMBER + "]:</p>\n";
            for(int i = 0; i < mergingJiraIssues.size(); i++) {
                def jiraIssue = mergingJiraIssues.get(i)
                def jiraIssueResult = mergingJiraIssuesSuccessResults.get(jiraIssue);
                if(jiraIssueResult) {
                    body += "${jiraIssue} : Success\n"
                } else {
                    body += "${jiraIssue} : Failure\n"
                }
            }
            body += "<p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"
            emailext (subject: subjectMail, body: body, to: mailTo)
        }
    }
}