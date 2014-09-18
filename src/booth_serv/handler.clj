(ns booth-serv.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.core.async :as async :refer [chan go-loop put! <!]]
            [toolbelt.core :refer [flip]]))

(def connected-clients (atom []))

(def notification-chan (chan))

(defn create-server-socket [port]
  (let [server (java.net.ServerSocket. port)]
    server))

(defn listen-on-server-socket [server new-clients-chan]
  (println "accepting connections")
  (go-loop []
           (let [client (.accept server)]
             (println "Connected to a new client")
             (put! new-clients-chan client)
             (println "Accepting further clients")
             (recur))))

(defn remove-client [client]
  (println "removing client" client)
  (swap! connected-clients (flip remove) #(= client %)))

(defn notify-client [client]
  (let [stream (.getOutputStream client)
        msg (str (-> (java.util.Date.) .toString) \newline)]
    (try
      (println "notifying clients")
      (.write stream (.getBytes msg))
      (catch Exception e
        (println "Error while writing to a client. Closing it")
        (remove-client client)))))

(defn notify-all-clients [clients]
  (doseq [client clients]
    (try
      (if (and (.isBound client)
               (.isConnected client)
               (not (.isClosed client)))
        (notify-client client)
        (remove-client client))

      (catch Exception e
        (.printStackTrace e)))))

(defn handle-incoming-notification [request]
  (notify-all-clients @connected-clients)
  "okay")

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/debug/notify" [] handle-incoming-notification)
  (POST "/" [] handle-incoming-notification)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn add-incoming-connections [c]
  (go-loop []
           (let [newbie (<! c)]
             (println "Welcome newbie")
             (swap! connected-clients conj newbie)
             (recur))))

(defn -main [& args]
  (println "Starting server")
  (let [server (create-server-socket 8888)
        incoming (chan)]

    (listen-on-server-socket server incoming)
    (add-incoming-connections incoming)))

(-main)
