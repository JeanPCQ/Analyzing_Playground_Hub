// --- Step 1: Sample dataset ---
let productName = "Sample_Product"; // default product

const salesData = [
  { month: "Jan-23", sales: 200 },
  { month: "Feb-23", sales: 205 },
  { month: "Mar-23", sales: 295 },
  { month: "Apr-23", sales: 203 },
  { month: "May-23", sales: 207 },
  { month: "Jun-23", sales: 198 },
  { month: "Jul-23", sales: 202 },
  { month: "Aug-23", sales: 206 },
  { month: "Sep-23", sales: 199 },
  { month: "Oct-23", sales: 204 },
  { month: "Nov-23", sales: 201 },
  { month: "Dec-23", sales: 205 }
];

// --- Forecasting Functions ---
function movingAverage(arr, windowSize) {
  let result = [];
  for (let i = 0; i < arr.length; i++) {
    if (i < windowSize) result.push(null);
    else {
      const slice = arr.slice(i - windowSize, i);
      result.push(slice.reduce((a,b)=>a+b,0)/windowSize);
    }
  }
  return result;
}

function linearRegression(y) {
  const x = [...Array(y.length).keys()];
  const n = y.length;
  const sumX = x.reduce((a,b)=>a+b,0);
  const sumY = y.reduce((a,b)=>a+b,0);
  const sumXY = x.reduce((a,b,i)=> a + b*y[i],0);
  const sumX2 = x.reduce((a,b)=>a + b*b,0);
  const slope = (n*sumXY - sumX*sumY)/(n*sumX2 - sumX*sumX);
  const intercept = (sumY - slope*sumX)/n;
  return x.map(xi => intercept + slope*xi);
}

function quantileFilteredForecast(y) {
  const sorted = [...y].sort((a,b)=>a-b);
  const Q1 = sorted[Math.floor((sorted.length-1)*0.25)];
  const Q3 = sorted[Math.floor((sorted.length-1)*0.75)];
  const IQR = Q3 - Q1;
  const filteredIndices = y.map((val,i) => (val >= Q1 - 1.5*IQR && val <= Q3 + 1.5*IQR) ? i : null).filter(i => i!==null);
  const filteredX = filteredIndices;
  const filteredY = filteredIndices.map(i => y[i]);
  const n = filteredY.length;
  const sumX = filteredX.reduce((a,b)=>a+b,0);
  const sumY = filteredY.reduce((a,b)=>a+b,0);
  const sumXY = filteredX.reduce((a,b,i)=>a + b*filteredY[i],0);
  const sumX2 = filteredX.reduce((a,b)=>a + b*b,0);
  const slope = (n*sumXY - sumX*sumY)/(n*sumX2 - sumX*sumX);
  const intercept = (sumY - slope*sumX)/n;
  return [...Array(y.length).keys()].map(xi => intercept + slope*xi);
}

function calculateMAPE(actual, predicted) {
  let errorSum = 0;
  let count = 0;
  for (let i = 0; i < actual.length; i++) {
    if (predicted[i] !== null) {
      errorSum += Math.abs((actual[i] - predicted[i]) / actual[i]);
      count++;
    }
  }
  return (errorSum / count) * 100;
}

// --- Step 2: Initialize Chart ---
const ctx = document.getElementById('forecastChart').getContext('2d');
const chart = new Chart(ctx, {
  type: 'line',
  data: {
    labels: salesData.map(d => d.month),
    datasets: [
      { label: 'Actual Sales', data: salesData.map(d=>d.sales), borderColor: 'blue', fill: false },
      { label: 'Naive Forecast', data: [], borderColor: 'red', borderDash:[5,5], fill: false },
      { label: '3-Month Moving Avg', data: [], borderColor: 'green', borderDash:[5,5], fill: false },
      { label: 'Linear Regression', data: [], borderColor: 'orange', borderDash:[5,5], fill: false },
      { label: 'Quantile-Filtered Regression', data: [], borderColor: 'purple', borderDash:[5,5], fill: false }
    ]
  },
  options: {
    responsive: true,
    plugins: { legend: { position: 'bottom' }, title: { display: true, text: `Forecast Model Comparison - ${productName}` } },
    scales: {
      x: { title: { display: true, text: 'Month' } },
      y: { title: { display: true, text: 'Sales' } }
    }
  }
});

// --- Step 3: CSV Upload & Dropdown ---
let uploadedData = [];

document.getElementById('fileInput').addEventListener('change', function(e){
  const file = e.target.files[0];
  const messageEl = document.getElementById('fileMessage');
  if(!file) return;

  const reader = new FileReader();
  reader.onload = function(evt){
    const text = evt.target.result;
    const rows = text.trim().split('\n');
    
    if(rows.length > 101){
      messageEl.textContent = "❌ Error: File exceeds 100 rows limit!";
      return;
    }
    messageEl.textContent = "✅ File uploaded successfully!";
    
    const headers = rows[0].split(',').map(h => h.trim());
    uploadedData = rows.slice(1).map(r => {
      const cols = r.split(',');
      return { date: cols[0].trim(), item: cols[1].trim(), value: parseFloat(cols[2].trim()) };
    });

    const items = [...new Set(uploadedData.map(d=>d.item))];
    const productSelect = document.getElementById('productSelect');
    productSelect.innerHTML = items.map(i=>`<option value="${i}">${i}</option>`).join('');

    updateForecast(items[0]);
  };
  reader.readAsText(file);
});

// --- Step 4: Update Forecast Function ---
function updateForecast(itemName){
  const data = uploadedData.length > 0 ? uploadedData.filter(d=>d.item===itemName) : salesData;
  const labels = data.map(d=>d.date || d.month);
  const actual = data.map(d=>d.value || d.sales);

  const naiveForecast = Array(actual.length).fill(actual[actual.length-2]);
  const movingAvgForecast = movingAverage(actual,3);
  const regressionForecast = linearRegression(actual);
  const quantileForecast = quantileFilteredForecast(actual);

  const mapeNaive = calculateMAPE(actual, naiveForecast);
  const mapeMA = calculateMAPE(actual, movingAvgForecast);
  const mapeLR = calculateMAPE(actual, regressionForecast);
  const mapeQF = calculateMAPE(actual, quantileForecast);

  const mapeScores = { "Naive": mapeNaive, "Moving Average": mapeMA, "Linear Regression": mapeLR, "Quantile Regression": mapeQF };
  const bestModel = Object.keys(mapeScores).reduce((a,b)=>mapeScores[a]<mapeScores[b]?a:b);

  document.getElementById('mapeOutput').innerHTML = `
    ✅ Best Model: ${bestModel}<br><br>
    📊 MAPE Scores:<br>
    Naive Forecast: ${mapeNaive.toFixed(2)}%<br>
    Moving Average: ${mapeMA.toFixed(2)}%<br>
    Linear Regression: ${mapeLR.toFixed(2)}%<br>
    Quantile-Filtered Regression: ${mapeQF.toFixed(2)}%
  `;

  chart.data.labels = labels;
  chart.data.datasets[0].data = actual;
  chart.data.datasets[1].data = naiveForecast;
  chart.data.datasets[2].data = movingAvgForecast;
  chart.data.datasets[3].data = regressionForecast;
  chart.data.datasets[4].data = quantileForecast;
  chart.options.plugins.title.text = `Forecast Model Comparison - ${itemName}`;
  chart.update();
}

// Dropdown event listener
document.getElementById('productSelect').addEventListener('change', e=>updateForecast(e.target.value));

// --- Step 5: Initialize with sample product ---
updateForecast(productName);
