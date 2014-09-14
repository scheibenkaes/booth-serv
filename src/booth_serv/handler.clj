(ns booth-serv.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (POST "/" [] (fn [_]
                 (println "got a request")))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
