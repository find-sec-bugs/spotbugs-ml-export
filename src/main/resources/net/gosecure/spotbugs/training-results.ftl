<html>
    <head>
        <title>Classifier training stats</title>
    </head>
    <body>
    	<h1>Classifier training stats :</h1>
    	<p>Recall : ${recall}</p>      
    	<p>Precision : ${precision}</p>
    	<p>F-measure : ${fmeasure}</p>
    	<p>Accuracy : ${accuracy}</p>
    	<p>Confusion Matrix :</p>
    	<table>
            <tr>
                <th>BAD</th>
                <th>GOOD</th>
                <td><-- classified as</td>
            </tr>
            <tr>
                <td align="center">${badbad}</td>
                <td align="center">${badgood}</td>
                <th>BAD</th>
            </tr>
            <tr>
                <td align="center">${goodbad}</td>
                <td align="center">${goodgood}</td>
                <th>GOOD</th>
            </tr>
        </table>
    </body>
</html>