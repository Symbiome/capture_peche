ogr2ogr -f PostgreSQL PG:"host=localhost dbname=fishola user=postgres password=whatever" \
  "data/TronconHydrographique_FXX-gpkg/TronconHydrographique_FXX.gpkg" \
  -nln river_sections \
  -t_srs EPSG:4326 \
  -dim XY \
  -lco GEOMETRY_NAME=geom \
  -lco FID=id \
  -progress
