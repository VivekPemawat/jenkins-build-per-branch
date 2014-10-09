package com.entagen.jenkins
import groovyx.net.http.RESTClient
import org.apache.http.HttpRequestInterceptor

import java.util.regex.Pattern

class GitApi {
    String gitUrl
    Pattern branchNameFilter = null

    RESTClient restClient
    HttpRequestInterceptor requestInterceptor


    /*public void setJenkinsServerUrl(String jenkinsServerUrl) {
        if (!jenkinsServerUrl.endsWith("/")) jenkinsServerUrl += "/"
        this.jenkinsServerUrl = jenkinsServerUrl
        this.restClient = new RESTClient(jenkinsServerUrl)
    }*/

   /* public void addBasicAuth(String jenkinsServerUser, String jenkinsServerPassword) {
        String gitUrlCopy=gitUrl;

        if (!gitUrl.endsWith("/")) gitUrlCopy += "/"
       // this.jenkinsServerUrl = jenkinsServerUrl
        this.restClient = new RESTClient("https://github.corp.inmobi.com/")
        println "use basic authentication added for github"

        this.requestInterceptor = new HttpRequestInterceptor() {
            void process(HttpRequest httpRequest, HttpContext httpContext) {
                def auth = jenkinsServerUser + ':' + jenkinsServerPassword
                httpRequest.addHeader('Authorization', 'Basic ' + auth.bytes.encodeBase64().toString())
            }
        }

        this.restClient.client.addRequestInterceptor(this.requestInterceptor)
    }
*/





    protected boolean getCheck(Map map) {
        // get is destructive to the map, if there's an error we want the values around still
        Map mapCopy = map.clone() as Map
        def response

        assert mapCopy.path != null, "'path' is a required attribute for the GET method"

        try {
            response = restClient.get(map)
            if(response.status==200) return true;
            else return false;

            /// response.
        } catch (Exception ex) {
            println "Unable to connect to host: $gitUrl"
            return false;
            // throw ex
        }

        // assert response.status < 400
        return false;
    }



    public String  checkLinkExists(String url) {
        String commandPOM ="GET "+URL+"/pom.xml -s -d";
          if(commandPOM.execute().text.contains("200")) {
              println commandPOM.execute().text;
              println "entered into java based"
              return "javabasedproject";
              //return true
          }

        String commandpython ="GET "+URL+"/setup.py -s -d";
        if(commandpython.execute().text.contains("200")) {

            return "pythonprject";
            //return true
        }

        String commandgradle ="GET "+URL+"/build.gradle -s -d";
        if(commandPOM.execute().text.contains("200")) {
            return "gradlebasedproject";
            //return true
        }
return "None";

    }
    public List<String> getBranchNames() {
        String command = "git ls-remote --heads ${gitUrl}"
        List<String> branchNames = []

        eachResultLine(command) { String line ->
            String branchNameRegex = "^.*\trefs/heads/(.*)\$"
            String branchName = line.find(branchNameRegex) { full, branchName -> branchName }
            Boolean selected = passesFilter(branchName)
            println "\t" + (selected ? "* " : "  ") + "$line"
            // lines are in the format of: <SHA>\trefs/heads/BRANCH_NAME
            // ex: b9c209a2bf1c159168bf6bc2dfa9540da7e8c4a26\trefs/heads/master
            if (selected) branchNames << branchName
        }

        return branchNames
    }

    public Boolean passesFilter(String branchName) {
        if (!branchName) return false
        if (!branchNameFilter) return true
        return branchName ==~ branchNameFilter
    }

    // assumes all commands are "safe", if we implement any destructive git commands, we'd want to separate those out for a dry-run
    public void eachResultLine(String command, Closure closure) {
        println "executing command: $command"
        def process = command.execute()
        def inputStream = process.getInputStream()
        def gitOutput = ""

        while(true) {
          int readByte = inputStream.read()
          if (readByte == -1) break // EOF
          byte[] bytes = new byte[1]
          bytes[0] = readByte
          gitOutput = gitOutput.concat(new String(bytes))
        }
        process.waitFor()

        if (process.exitValue() == 0) {
            gitOutput.eachLine { String line ->
               closure(line)
          }
        } else {
            String errorText = process.errorStream.text?.trim()
            println "error executing command: $command"
            println errorText
            throw new Exception("Error executing command: $command -> $errorText")
        }
    }

}
