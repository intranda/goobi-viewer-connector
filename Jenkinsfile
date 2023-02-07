pipeline {

  agent {
    docker {
      label 'controller'
      image 'nexus.intranda.com:4443/goobi-viewer-testing-index:latest'
      args '-v $HOME/.m2:/var/maven/.m2:z -v $HOME/.config:/var/maven/.config -v $HOME/.sonar:/var/maven/.sonar -u 1000 -ti -e _JAVA_OPTIONS=-Duser.home=/var/maven -e MAVEN_CONFIG=/var/maven/.m2'
      registryUrl 'https://nexus.intranda.com:4443/'
      registryCredentialsId 'jenkins-docker'
    }
  }

  options {
    buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '15', daysToKeepStr: '90', numToKeepStr: '')
  }

  stages {
    stage('prepare') {
      steps {
        sh 'git clean -fdx'
      }
    }
    stage('build') {
      steps {
              sh 'mvn -f goobi-viewer-connector/pom.xml -DskipTests=false -DskipDependencyCheck=false clean verify -U'
              recordIssues enabledForFailure: true, aggregatingResults: true, tools: [java(), javaDoc()]
              dependencyCheckPublisher pattern: '**/target/dependency-check-report.xml'
      }
    }
    stage('sonarcloud') {
      when {
        anyOf {
          tag "v*"
          branch 'sonar_*'
        }
      }
      steps {
        withCredentials([string(credentialsId: 'jenkins-sonarcloud', variable: 'TOKEN')]) {
          sh 'mvn -f goobi-viewer-connector/pom.xml verify sonar:sonar -Dsonar.login=$TOKEN'
        }
      }
    }
    stage('deployment of artifacts to maven repository') {
      when {
        anyOf {
          tag "v*"
          branch 'develop'
        }
      }
      steps {
        sh 'mvn -f goobi-viewer-connector/pom.xml deploy'
      }
    }
  }
  post {
    always {
      junit "**/target/surefire-reports/*.xml"
      step([
        $class           : 'JacocoPublisher',
        execPattern      : 'goobi-viewer-connector/target/jacoco.exec',
        classPattern     : 'goobi-viewer-connector/target/classes/',
        sourcePattern    : 'goobi-viewer-connector/src/main/java',
        exclusionPattern : '**/*Test.class'
      ])
    }
    success {
      archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
    }
    changed {
      emailext(
        subject: '${DEFAULT_SUBJECT}',
        body: '${DEFAULT_CONTENT}',
        recipientProviders: [requestor(),culprits()],
        attachLog: true
      )
    }
    failure {
      emailext(
        subject: '${DEFAULT_SUBJECT}',
        body: '${DEFAULT_CONTENT}',
        to: 'andrey.kozhushkov@intranda.com',
        attachLog: true
      )
    }
  }
}
/* vim: set ts=2 sw=2 tw=120 et :*/

