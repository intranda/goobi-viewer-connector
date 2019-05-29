# Goobi viewer - Connector
> Connector application as part of the highly flexible digital library framework - made by the Goobi developer team


## Connector for the digital library framework
The Goobi viewer connector provides several connectivity methods to the indexed data content of the Goobi viewer. Currently the connector supports two ways to let other machines communicate with the Goobi viewer:

### OAI-PMH
The embedded OAI-PMH connector provides a standard OAI interface for harvesters. Dependent of the configuration it offers the following formats for receiving the content:

- METS/MODS
- Dublin Core
- MARC XML
- LIDO
- Epicur
- ESE

### SRU
The SRU connector offers the possibility to request specific information from the data store of the Goobi viewer. This is used for example by the reference manager program [Citavi](https://www.citavi.com/en/) and others to embed the Goobi viewer data store into the databases that these programs search through.


## Documentation
A complete documentation of the Goobi viewer can be found using this URL:
<https://docs.intranda.com/>


## Technical background
The Goobi viewer indexer is part of the Goobi viewer project which consists of multiple packages:

| Package | Function |
| ------ | ------ |
| [Goobi viewer - Core](https://github.com/intranda/goobi-viewer-core) | Core functionality of the viewer application|
| [Goobi viewer - Indexer](https://github.com/intranda/goobi-viewer-indexer) | Indexing application to fill the Solr search index with metadata information |
| [Goobi viewer - Connector](https://github.com/intranda/goobi-viewer-connector) | Connectors for different use cases (incl. OAI-PMH, SRU)|
| [Goobi viewer - Theme Reference](https://github.com/intranda/goobi-viewer-theme-reference) | Reference Theme for the user interface |
| [Goobi viewer - Theme Boilerplate](https://github.com/intranda/goobi-viewer-theme-boilerplate) | Boilerplate for creating new Themes for the user interface |


## Installation
The installation can be done on any operating system as the software is based on Java. A detailed explanation how to install the viewer will follow later on. In the mean time please get in touch with us via <info@intranda.com>


## Release History
Detailed monthly digests can be found using this URL:
<https://docs.intranda.com/goobi-viewer-digests-de/>


## Developer team
intranda GmbH
Bertha-von-Suttner-Str. 9
37085 Göttingen
Germany


## Contact us
If you would like to get in touch with the developers please use the following contact details:

| Contact |Address |
| ------ | ------ |
| Website | <http://www.intranda.com> |
| Mail | <info@intranda.com> |
| Twitter intranda | <http://twitter.com/intranda> |
| Twitter Goobi | <http://twitter.com/goobi> |
| Github | <https://github.com/intranda> |


## Licence
The Goobi viewer connector is released under the license GPL2 or later.
Please see ``LICENSE`` for more information.


## Contributing
1. Fork it (<https://github.com/intranda/goobi-viewer-connector/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Commit your changes (`git commit -am 'Add some fooBar'`)
4. Push to the branch (`git push origin feature/fooBar`)
5. Create a new Pull Request

