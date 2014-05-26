testInProgress-spock-client
===========================

Spock unit test framework client for Jenkins TestIn Progress plugin.

This is the client code for Spock integration to the testInProgress Plugin of Jenkins.

Just follow the following steps to integrate this into your existing framework.
- Download and install TestInProgress plugin in your jenkins. More information available at "https://wiki.jenkins-ci.org/display/JENKINS/Test+In+Progress+Plugin"
- Add the Jenkisn repository "http://repo.jenkins-ci.org/public/" in your list of respositories in case you are using gradle, ivy or maven.
- Add dependency of 
groupid: "org.imaginea.jenkins.plugins"
artifactId: "testInProgress-spock-client"
version: <current version>
- Just add a file named "org.spockframework.runtime.extension.IGlobalExtension" under folder "src/test/resources/META_INF/services".
- Add the line "org.imaginea.jenkins.testinprogress.spock.SpockTestInProgressExtension" to the above mentioned file.
- Enable the testInProgress plugin for your Jenkisn job.

Now whenever you run your tests using gradle or maven on Jenkins you can see the running status of your spock tests under testInProgress plugin.

Note:
Spock does not give the information of the total tests that are going to be executed in a suite. Hence the count shown on the plugin is a workaround.
