<!DOCTYPE html>
<html>
     <head>
        <title>Classification Results</title>
        <meta charset="UTF-8">
        </head>
    <body>
    	<h1>Classifier Results</h1><br/>
        <h3>Issues with less than 90% confidence (${numberIssues} issues) :</h3>      
    	<table>
            <tr>
                <th>Source File</th>  
                <th>Line Number</th>
                <th>Bug Type</th>
            </tr>        

            <#list issues as issue>
                <tr>
                    <td>${issue.sourceFile}</td> 
                    <td>${issue.line}</td>
                    <td>${issue.bugType}</td>
                </tr>
            </#list>
        </table>       
    </body>
</html>