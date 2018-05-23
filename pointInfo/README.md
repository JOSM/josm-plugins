![](http://www.kyralovi.cz/tmp/josm/pointInfo_20170128.png)

---

# PointInfo plugin

This plugin shows all available information for clicked point from external database.
Only Czech RUIAN and Spanish Cadastre Web Services modules are available at this moment.

Plugin could be easy extend to show another data source.

## Author

 * Marián Kyral <mkyral@email.cz>

## Contributors

  * Javier Sánchez Portero <javiersanp@gmail.com> (Spanish Cadastre Web Services module)

## Websites

 * [OSM wiki](https://wiki.openstreetmap.org/wiki/JOSM/Plugins/pointinfo)
 * [JOSM svn](https://trac.openstreetmap.org/browser/subversion/applications/editors/josm/plugins/pointInfo)
 * [Github](https://github.com/mkyral/josm-pointInfo)

## Licence:

 * GPL v2 or later

---
### The RUIAN module

 * Shows data about building, addresses, streets,  parcels and cadastral area from Czech RUIAN registry (https://wiki.openstreetmap.org/wiki/RUIAN)

 * Additional actions are available :
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/open-external-link.png)] Open external site
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/open-external-link-kn.png)] Open external site (Katastr nemovitostí)
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/copy-tags.png)] Copy tags to clipboard
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/create-addr.png)] Create an address point on position where was clicked
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/create-addr-ruian.png)] Create an address point on position defined in RUIAN
    * [![](https://raw.githubusercontent.com/mkyral/josm-pointInfo/master/images/dialogs/create-bug-report.png)] Report an issue with building

### The Spanish Cadastre Web Services module

  * Easy access the Spanish Cadastre Web Services (only Cadastre photographs at the moment).

---
### The interface:

- Input is position, output html string that is shown on message.
- Optionally you can define special links (file://...) that will be sent back to the module to the performAction method

```java

    /**
     * Get a information about given position from external database.
     * @param pos Position on the map
     */
    public void prepareData(LatLon pos) {
    }

    /**
     * Return Html text representation
     * @return String htmlText
     */
    public String getHtml() {
    }

    /**
     * Perform given action
     *  e.g.: copy tags to clipboard
     * @param act Action to be performed
     */
    public void performAction(String act) {
    }

```
