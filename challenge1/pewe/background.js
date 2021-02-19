/**
 *
 * Online browser anonymizer by Eline Brader (s2674483) and Lars Ran (s1403192).
 *
 * A tool made for challenge 1 of Network Systems at Universiteit Twente.
 */
function onrequest(req) {
  // This function will be called everytime the browser is about to send out an http or https request.
  // The req variable contains all information about the request.
  // If we return {}  the request will be performed, without any further changes
  // If we return {cancel:true} , the request will be cancelled.
  // If we return {requestHeaders:req.requestHeaders} , any modifications made to the requestHeaders (see below) are sent.

  // log what file we're going to fetch:
  console.log("Loading: " + req.method +" "+ req.url + " "+ req.type);

  // Print if there are interesting URL classifications present.
  if(req.urlClassification.thirdParty.length !== 0
      || req.urlClassification.firstParty.length !== 0){
    console.log(req.urlClassification);
  }

  // Will not send the request whenever the thirdParty classification contains "tracking_ad" or "tracking_analytics"
  if(req.urlClassification.thirdParty.includes("tracking_ad")
      || req.urlClassification.thirdParty.includes("tracking_analytics")){
    console.log("Not today");
    return{cancel:true};
  }

  // let's do something special if an image is loaded:
  if (req.type=="image") {
     console.log("Ooh, it's a picture!");
  }

  // req also contains an array called requestHeaders containing the name and value of each header.
  // You can access the name and value of the i'th header as req.requestHeaders[i].name and req.requestHeaders[i].value ,
  // with i from 0 up to (but not including) req.requestHeaders.length .

  // Prints the headers and its values. Also edits the "User-Agent" header to anonymize oneself.
  for(let i = 0; i < req.requestHeaders.length; i++){
    if(req.requestHeaders[i].name == "User-Agent"){
      req.requestHeaders[i].value = "a Potato";
    }
    console.log("\t" + req.requestHeaders[i].name + ": " + req.requestHeaders[i].value);
  }

  return {requestHeaders:req.requestHeaders};
}


// no need to change the following, it just makes sure that the above function is called whenever the browser wants to fetch a file
browser.webRequest.onBeforeSendHeaders.addListener(
  onrequest,
  {urls: ["<all_urls>"]},
  ["blocking", "requestHeaders"],
);

