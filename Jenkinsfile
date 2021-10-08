pipeline {
  agent none

  options {
    buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '15', daysToKeepStr: '90', numToKeepStr: '')
  }

  stages {
    stage('prepare') {
      agent any
      steps {
        sh 'git clean -fdx'
      }
    }
    stage('build') {
      agent {
        docker {
          image 'maven:3-jdk-11'
          args '-v $HOME/.m2:/var/maven/.m2:z -u 1000 -ti -e _JAVA_OPTIONS=-Duser.home=/var/maven -e MAVEN_CONFIG=/var/maven/.m2'
        }
      }
      steps {
        sh 'mvn -f goobi-viewer-connector/pom.xml clean install'
        recordIssues enabledForFailure: true, aggregatingResults: true, tools: [java(), javaDoc()]
        archiveArtifacts artifacts: '**/target/*.war', fingerprint: true, onlyIfSuccessful: true
        junit "**/target/surefire-reports/*.xml"
        step([
          $class           : 'JacocoPublisher',
          execPattern      : 'goobi-viewer-connector/target/jacoco.exec',
          classPattern     : 'goobi-viewer-connector/target/classes/',
          sourcePattern    : 'goobi-viewer-connector/src/main/java',
          exclusionPattern : '**/*Test.class'
        ])
      }
    }
    stage('sonarcloud') {
      agent any
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
            dockerimage_public = docker.build("intranda/goobi-viewer-connector:${BRANCH_NAME}-${env.BUILD_ID}_${env.GIT_COMMIT}")
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
      when {
        tag "v*"
      }
      steps{
        script {
          docker.withRegistry('https://nexus.intranda.com:4443','jenkins-docker'){
            dockerimage.push("${env.TAG_NAME}-${env.BUILD_ID}")
            dockerimage.push("${env.TAG_NAME}")
            dockerimage.push("latest")
          }
        }
      }
    }
    stage('publish develop image to Docker Hub'){
      agent any
      when {
        branch 'develop'
      }
      steps{
        script{
          docker.withRegistry('','0b13af35-a2fb-41f7-8ec7-01eaddcbe99d'){
            dockerimage_public.push("${env.BRANCH_NAME}")
          }
        }
      }
    }
    stage('publish production image to Docker Hub'){
      agent any
      when {
        tag "v*"
      }
      steps{
        script{
          docker.withRegistry('','0b13af35-a2fb-41f7-8ec7-01eaddcbe99d'){
            dockerimage_public.push("${env.TAG_NAME}")
            dockerimage_public.push("latest")
          }
        }
      }
    }
  }
  post {
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
