const city = "East Windsor";
const state = "NJ"
const vin = "2T3DWRFV3LW077677"
const apiKey = "oPOMMxOeJCBe6FHJlRkXBSzufXX3KHU6";
const url = `https://mc-api.marketcheck.com/v2/predict/car/us/marketcheck_price/comparables/decode?api_key=${apiKey}&vin=${vin}&miles=76189&dealer_type=independent&city=${city}&state=${state}`;

fetch(url)
  .then(function(response){
    return response.json();
  })
  .then(function(data){
    //document.getElementById("output").textContent = JSON.stringify(data, null, 2);
    document.getElementById("output").textContent = 
      `Make:  ${data.decode.make}\n` +
      `Model: ${data.decode.model}\n` +
      `Body:  ${data.decode.body_type}\n` + 
      `Type:  ${data.decode.vehicle_type}\n` +
      `Fuel:  ${data.decode.fuel_type}\n`
      ;
  });