Web Crawler
===

### A simple single threaded web crawler 

The web crawler is designed to visit every reachable page for a given domain and then extract the urls for any asset on that page i.e. image, stylesheets and javascript.

### How to run

Use the `sbt run` command to start the application, then when prompted  enter the starting url that you wish to crawl, e.g. https://scrapethissite.com/ 

### Design Assumptions

- The website given to crawl is of a suitable size
- All of the website's assets can be held in memory during the crawl operation 
  
