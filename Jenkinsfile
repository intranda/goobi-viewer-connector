pipeline {

  agent {
    docker {
      image 'maven:3.6.3-openjdk-11'
      args '-v $HOME/.m2:/var/maven/.m2:z -v $HOME/.config:/var/maven/.config -v $HOME/.sonar:/var/maven/.sonar -u 1000 -ti -e _JAVA_OPTIONS=-Duser.home=/var/maven -e MAVEN_CONFIG=/var/maven/.m2'
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
              sh 'mvn -f goobi-viewer-connector/pom.xml clean install'
              recordIssues enabledForFailure: true, aggregatingResults: true, tools: [java(), javaDoc()]
      }
    }
    stage('sonarcloud') {
      when {
        anyOf {
          branch 'sonar_*'
        }
      }
      steps {
        withCredentials([string(credentialsId: 'jenkins-sonarcloud', variable: 'TOKEN')]) {
          sh 'mvn -f goobi-viewer-connector/pom.xml verify sonar:sonar -Dsonar.login=$TOKEN'
        }
      }
    }
    stage('build docker image') {
      agent any
      steps {
        script{
          docker.withRegistry('https://nexus.intranda.com:4443','jenkins-docker'){
            dockerimage = docker.build("goobi-viewer-connector:${BRANCH_NAME}-${env.BUILD_ID}_${env.GIT_COMMIT}")
          }
        }
      }
    }
    stage('basic tests'){
      agent any
      steps{
        script {
          dockerimage.inside {
            sh 'test -d /usr/local/tomcat/webapps/M2M && echo "/usr/local/tomcat/webapps/M2M missing or no directory"'
            sh 'test -d /opt/digiverso/viewer/oai || echo "/opt/digiverso/viewer/oai missing or no directory"'
            sh 'test -f /config_oai.xml.template || echo "/config_oai.xml.template missing"'
            sh 'envsubst -V'
          }
        }
      }
    }
    stage('publish docker devel image to internal repository'){
      agent any
      steps{
        script {
          docker.withRegistry('https://nexus.intranda.com:4443','jenkins-docker'){
            dockerimage.push("${env.BRANCH_NAME}-${env.BUILD_ID}_${env.GIT_COMMIT}")
            dockerimage.push("${env.BRANCH_NAME}")
          }
        }
      }
    }
    stage('publish docker production image to internal repository'){
      agent any
      when { branch 'master' }
      steps{
        script {
          docker.withRegistry('https://nexus.intranda.com:4443','jenkins-docker'){
            dockerimage.push("${env.TAG_NAME}-${env.BUILD_ID}_${env.GIT_COMMIT}")
            dockerimage.push("latest")
          }
        }
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
      archiveArtifacts artifacts: '**/target/*.war, */src/main/webapp/oai2.xsl', fingerprint: true
    }
    changed {
      emailext(
        subject: '${DEFAULT_SUBJECT}',
        body: '${DEFAULT_CONTENT}',
        recipientProviders: [requestor(),culprits()],
        attachLog: true
      )
    }
  }
}
/* vim: set ts=2 sw=2 tw=120 et :*/
