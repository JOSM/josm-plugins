# Regression Readme
## Generating nodes.json, ways.json, and relations.json
In order to generate these jsons, you will want a list of the objects. You can
get this by selecting the objects in JOSM and copying them.

Example:

```
node 1
node 2
```

Once you have the list, you can run the following command:
```shell
# Note: `pbpaste` is for mac, `xclip -selection clipboard -o` is for x11 linux, and `wl-paste` (from `wl-clipboard`) is for linux wayland
$ wl-paste | \
  awk '{print "https://api.openstreetmap.org/api/0.6/$1/" $2 "/history.json" " -o " $1 "_" $2 ".json"}' | \
  xargs curl -L
$ jq -s '.[0].elements=([.[].elements]|flatten)|.[0]' node_*.json > nodes.json
$ jq -s '.[0].elements=([.[].elements]|flatten)|.[0]' way_*.json > ways.json
$ jq -s '.[0].elements=([.[].elements]|flatten)|.[0]' relation_*.json > relations.json
```