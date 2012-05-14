function(value, key, _) {
  var data = Riak.mapValuesJson(value)[0];

  return [(data.price * data.quantity)];
}