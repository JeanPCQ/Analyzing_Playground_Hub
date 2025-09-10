const axios = require('axios');

const apiKey = 'YOUR_API_KEY_HERE';
const vin = 'SOMEVIN123';
const city = 'New York';
const state = 'NY';
const miles = 12000;
const dealer_type = 'independent';

const params = {
    api_key: apiKey,
    vin: vin,
    miles: miles,
    dealer_type: dealer_type,
    city: city,
    state: state
};

axios.get('https://mc-api.marketcheck.com/v2/predict/car/us/marketcheck_price/comparables', { params })
    .then(response => {
        console.log('Car Value:', response.data.marketcheck_price);
        console.log('Raw API Response:', response.data);
    })
    .catch(error => {
        console.error('API Error:', error.message);
    });
