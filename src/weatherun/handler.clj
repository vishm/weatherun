(ns weatherun.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))


(def apikey "18493e8e77138f58")


(defn make_uri
    [city country]
    (str "http://api.wunderground.com/api/" apikey "/geolookup/forecast10day/q/" country "/" city ".json"))


(defn make-404-response
  [msg]
  {:status 404 :body msg}
  )

(defn make-200-response
  [data]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str data)})

(defn httpget
  [url]
  (try
    (let [response (client/get url)]
      (json/read-str (:body response) :key-fn keyword)
    )
    (catch Exception e { } )))

(defn forecast-info 
  [forecast]
  {
    :lowcelsius  (get-in forecast [:low :celsius])
    :highcelsius (get-in forecast [:high :celsius])
    :lowfahrenheit  (get-in forecast [:low :fahrenheit])
    :highfahrenheit (get-in forecast [:high :fahrenheit])
  })

(defn extract-core-info 
  [data]
  {    
    :area {
      :city (get-in data [:location :city])
      :country (get-in data [:location :country])
      :latitude (get-in data [:location :lat])
      :longitude (get-in data [:location :lon])
    }
    :forecast (map forecast-info (get-in data [:forecast :simpleforecast :forecastday]))
  })

(defn getweather
  [country city]
  (let [response (httpget (make_uri city country))]
  (if (and (contains? response :forecast) (contains? response :location))
    (make-200-response (extract-core-info response))
    (make-404-response "Invalid City,Country")
    )))



(defroutes app-routes
  (GET "/v1/Weather/:country/:city" [country city] (getweather country city))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

