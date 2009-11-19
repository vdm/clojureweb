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

; records a string (:expr) along with the result of its evaluation by
; repl, and anything written to *out* or *err* 
(defstruct history-item :expr :result :out :err)

; a list of history-items
(def history-items (atom ()))

(defn html-history-item [{:keys [expr result out err]}]
  (html [:div {:id "result"} [:p (str expr " = " result)]]
	[:div {:id "out"} [:p out]]
	[:div {:id "err"} [:p err]]
  )
)

(defn html-repl [history]
  (html [:form {:action "/repl", :method "post"}
          [:input {:type "text", :name "expr"}]
          [:input {:type "submit", :value "Submit"}]
        ]
        (map html-history-item history)
  )
)

(defn repl-get [request]
  "Handles a GET to /repl, returns a map with the :body key set to 
  a blank html form"
  {:body (html-repl @history-items)}
)

(defn repl-post [request]
"Handles a post to /repl, simply doing a read and then eval of the
 relevant string passed in the request parameter. Doesn't handle the
 exceptions potentially thrown by read or eval"
  (let [expr (:expr (:form-params request))
        result (str (eval (read-string expr)))
	new-item (struct history-item expr result)
	history (swap! history-items conj new-item)
       ]
    {:body (html-repl history)}
  )
)

(defn my-read [a b]
  "Just This"
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
  (let [expr (:expr (:form-params request))
        in (PushbackReader. (StringReader. expr))
        out (StringWriter.)
        err-writer (StringWriter.)
        err (PrintWriter. err-writer)]
    (repl-with {:in in :out out :err err}) 
    (swap! history-items
	conj (struct history-item expr (str out) (str err-writer)))
    {:body (html-repl @history-items)}
  )
)

(defroutes clojure-web
  (GET "/" (html [:h1 "Clojure " [:a {:href"/repl"} "REPL"]]))
  (GET "/repl" repl-get)
  (POST "/repl" repl-post-alt)
)


(defn run []
  (run-server {:port 8080}
    "/*" (servlet clojure-web))
)

(run)
