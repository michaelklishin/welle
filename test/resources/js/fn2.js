function (values, _) {
  values = Riak.filterNotFound(values);

  var acc = {};
  values.forEach(function(pair) {
    var k = pair[0], v = pair[1];
    if(typeof acc[k] === 'undefined') {
      acc[k] = 0;
    } else {
      acc[k] = acc[k] + v;
    };
  });
  return [acc];
}