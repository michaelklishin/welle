%% an example used in Riak Handbook by Mathias Meyer, modified to fit Elastisch fixtures
%% (we just take example documents from there)
{schema, [{version,       "1.1"},
          {n_val,         2},
          {default_field, "field"},
          {default_op,    "or"},
          {analyzer_factory, {erlang, text_analyzers, whitespace_analyzer_factory}}],
 [{field, [{name, "text"},
           {type, string},
           {required, true},
           {analyzer_factory, {erlang, text_analyzers, standard_analyzer_factory}}]},
  {field, [{name, "timestamp"},
           {type, date},
           {required, true},
           {analyzer_factory, {erlang, text_analyzers, noop_analyzer_factory}}]},
  {field, [{name, "username"},
           {type, string},
           {required, true},
           {analyzer_factory, {erlang, text_analyzers, noop_analyzer_factory}} ]},
  {dynamic_field, [{name, "*"}, {skip, true}]}
 ]}.
