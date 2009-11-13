(ns hello-www
  (:import (java.io PushbackReader StringReader
                    StringWriter PrintWriter))
  (:use compojure)
)

;(def in-str "")
;(def in (PushbackReader. (StringReader. in-str)))
;(def out (StringWriter. ))
;(def err-str-writer (StringWriter. ))
;(def err (PrintWriter. err-str-writer))

(defn html-repl [in out err]
  (html [:form {:action "/repl", :method "post"}
          [:input {:type "text", :name "in"}]
          [:input {:type "submit", :value "Submit"}]
        ]
	[:div {:id "result"} [:p (str in " = " out)]]
	[:div {:id "err"} [:p err]]
  )
)

(defn repl-get [request]
  "Handles a GET to /repl, returns a map with the :body key set to 
  a blank html form"
  {:body (html-repl "" "" "")}
)

(defn repl-with
  "Start a REPL with *in*, *out* and *err* bound to the streams given
   as arguments"
  [{:keys [in out err] :or {in *in* out *out* err *err*}}]
  (binding [*in* in
            *out* out
            *err* err]
    (clojure.main/repl)
  )
)

(defn repl-post-alt [request]
"Alternative method of handling a post to /repl, uses clojure.main/repl
 via repl-with function"
  (let [input-str (:in (:form-params request))
        in (PushbackReader. (StringReader. input-str))
        out (StringWriter.)
        err-writer (StringWriter.)
        err (PrintWriter. err-writer)]
    (repl-with {:in in :out out :err err}) 
    {:body (html-repl (.toString out) (.toString err-writer))}
  )
)

(defn repl-post [request]
"Handles a post to /repl"
  (let [in (:in (:form-params request))
        out (str (eval (read-string in)))
       ]
    {:body (html-repl in out "")}
  )
)

(defroutes clojure-web
  (GET "/" (html [:h1 "Clojure " [:a {:href"/repl"} "REPL"]]))
  (GET "/repl" repl-get)
  (POST "/repl" repl-post)
;  (POST "/repl" #(str (eval (read-string (code-str-from-request %1)))))
)

(defn run []
  (run-server {:port 8080}
    "/*" (servlet clojure-web))
)
(run)
