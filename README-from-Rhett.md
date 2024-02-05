# Intro

Here is the absolute minimum static kogito engine. I'll try to refine it more soon. 
The biggest disadvantage of this appliction is that it does not support directly executing decision services inside DMNs. 

# How I created it. 

First I took the kogito spring boot archetype. and ran mvn clean compile. then, I removed the kogito plugin from the pom.xml to disable the kogito java code generation. Then I moved all the generated kogito java code from the target folder into the src folder. those classes are in java.org.kie package. After that, I generalized rhett/App.java and added a new class: rhett/CustomController.java. 


# Low effort, high value recommendations. 

the static block of the DecisionModels.java class (line 22) is generated code. Currently, it has 2 hardcoded DMNs from when the last kogito generation code executed. These two DMNs are in the resources folder. You can change this code to pull DMNs from other sources. anywhere. any stream, from anywhere. 

The next recommendation is to create and populate a Map that contains the dmn names and namespaces while you load the DMNs in. then you can lookup the DMNNamespace in the Map, so it doesnt need to be passed in with the request. 

The two TrafficeViolation...java classes in org.kie package are the generated controllers. they have no use except for reference, debugging.

# Example request

{
    "DmnName" : "TrafficViolation-v1",
    "DmnNamespace" : "https://github.com/kiegroup/drools/kie-dmn/_A4BCA8B8-CF08-433F-93B2-A2598F19ECF1",
    "Driver" : {"Points":2},
       "Violation": {
           "Type": "speed",
           "Actual Speed":"140",
           "Speed Limit": 100
       }
}
