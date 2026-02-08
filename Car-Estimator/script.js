// A more robust data structure to hold both inputs and results for each analysis
const analyses = {
  1: {
    inputs: { vin: '', zip: '', avgTravel: '0', buyTerm: '', downPayment: '0', buyMonthly: '0', currentMiles: '0' },
    results: []
  },
  2: {
    inputs: { vin: '', zip: '', avgTravel: '0', buyTerm: '', downPayment: '0', buyMonthly: '0', currentMiles: '0' },
    results: []
  },
  3: {
    inputs: { vin: '', zip: '', avgTravel: '0', buyTerm: '', downPayment: '0', buyMonthly: '0', currentMiles: '0' },
    results: []
  }
};

let currentVehicle = 1; // Default active vehicle

// Map input IDs to keys in our data structure for easy saving and loading
const inputIds = ['vin', 'zip', 'avgTravel', 'buyTerm', 'downPayment', 'buyMonthly', 'currentMiles'];

// Helper function to save the current form's state to our 'analyses' object
function saveInputs(vehicleId) {
  if (!vehicleId) return;
  inputIds.forEach(id => {
    analyses[vehicleId].inputs[id] = document.getElementById(id).value;
  });
}

// Helper function to load a vehicle's state into the form
function loadInputs(vehicleId) {
  if (!vehicleId) return;
  inputIds.forEach(id => {
    document.getElementById(id).value = analyses[vehicleId].inputs[id];
  });
}

// Chart.js setup
let carChart;

function initChart() {
  const ctx = document.getElementById('carChart').getContext('2d');
  carChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: [],
      datasets: []
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: 'top' },
        title: { display: true, text: 'Car Purchase Analysis' }
      },
      scales: {
        y: { beginAtZero: true, title: { display: true, text: 'USD ($)' } },
        x: { title: { display: true, text: 'Year' } }
      }
    }
  });
}

// Generate table
function generateTable(data) {
  const resultsDiv = document.getElementById('results');
  const resultsWrapper = document.getElementById('resultsWrapper');
  resultsDiv.innerHTML = '';
  
  if (data.length === 0) {
    resultsWrapper.style.display = 'none';
    return;
  }

  // Show results section
  resultsWrapper.style.display = 'block';

  const table = document.createElement('table');
  table.style.borderCollapse = 'collapse';
  table.style.width = '100%';

  // Header
  const thead = document.createElement('thead');
  const headerRow = document.createElement('tr');
  ['Year', 'Miles', 'Investment', 'Car Value'].forEach(text => {
    const th = document.createElement('th');
    th.textContent = text;
    th.style.border = '1px solid #e2e8f0';
    th.style.padding = '12px';
    th.style.textAlign = 'left';
    th.style.fontWeight = '600';
    th.style.backgroundColor = '#f8fafc';
    th.style.color = '#334155';
    headerRow.appendChild(th);
  });
  thead.appendChild(headerRow);
  table.appendChild(thead);

  // Body
  const tbody = document.createElement('tbody');
  data.forEach((row, idx) => {
    const tr = document.createElement('tr');
    tr.style.backgroundColor = idx % 2 === 0 ? '#fff' : '#f8fafc';
    [row.Year, row.Miles, `$${row.Buy_Paid.toLocaleString()}`, `$${typeof row.Car_Value === 'number' ? row.Car_Value.toLocaleString() : row.Car_Value}`].forEach(cellData => {
      const td = document.createElement('td');
      td.textContent = cellData;
      td.style.border = '1px solid #e2e8f0';
      td.style.padding = '12px';
      td.style.color = '#334155';
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  resultsDiv.appendChild(table);
}

// Update chart for single vehicle
function updateChart(data, label) {
  if (!carChart) initChart();
  
  carChart.data.labels = data.map(d => `Year ${d.Year}`);
  carChart.data.datasets = [
    {
      label: `${label} Car Value`,
      data: data.map(d => d.Car_Value === 'N/A' || d.Car_Value === 'Error' ? null : Number(d.Car_Value)),
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59, 130, 246, 0.1)',
      fill: true,
      tension: 0.4,
      borderWidth: 2
    },
    {
      label: `${label} Investment`,
      data: data.map(d => d.Buy_Paid ?? null),
      borderColor: '#ef4444',
      backgroundColor: 'rgba(239, 68, 68, 0.1)',
      fill: true,
      tension: 0.4,
      borderWidth: 2
    }
  ];
  carChart.update();
}

// Update chart for all vehicles
function updateCompareChart() {
  if (!carChart) initChart();
  
  carChart.data.datasets = [];
  carChart.data.labels = [];

  const colors = [
    { border: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)' },
    { border: '#10b981', bg: 'rgba(16, 185, 129, 0.1)' },
    { border: '#8b5cf6', bg: 'rgba(139, 92, 246, 0.1)' }
  ];

  Object.entries(analyses).forEach(([key, analysis], idx) => {
    const data = analysis.results;
    if (data.length > 0) {
      // Use the longest dataset to define the x-axis labels (years)
      if (data.length > carChart.data.labels.length) {
        carChart.data.labels = data.map(d => `Year ${d.Year}`);
      }

      // Add the "Car Value" dataset for this vehicle (Solid Line)
      carChart.data.datasets.push({
        label: `Vehicle ${key} Car Value`,
        data: data.map(d => d.Car_Value === 'N/A' || d.Car_Value === 'Error' ? null : Number(d.Car_Value)),
        borderColor: colors[idx].border,
        backgroundColor: colors[idx].bg,
        fill: true,
        tension: 0.4,
        borderWidth: 2
      });

      // Add the "Investment" dataset for this vehicle (Dashed Line)
      carChart.data.datasets.push({
        label: `Vehicle ${key} Investment`,
        data: data.map(d => d.Buy_Paid ?? null),
        borderColor: colors[idx].border,
        borderDash: [5, 5],
        backgroundColor: 'transparent',
        fill: false,
        tension: 0.4,
        borderWidth: 2
      });
    }
  });

  carChart.update();
}

// Tab switching
document.querySelectorAll('.tab').forEach((btn, idx) => {
  btn.addEventListener('click', () => {
    // Save current inputs before switching
    saveInputs(currentVehicle);

    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (btn.textContent.includes("Vehicle")) {
      currentVehicle = idx + 1;
      loadInputs(currentVehicle);

      const currentAnalysis = analyses[currentVehicle];
      if (currentAnalysis.results.length > 0) {
        generateTable(currentAnalysis.results);
        updateChart(currentAnalysis.results, `Vehicle ${currentVehicle}`);
        document.getElementById('chartWrapper').style.display = 'block';
      } else {
        document.getElementById('results').innerHTML = "No data yet. Enter details and calculate.";
        document.getElementById('resultsWrapper').style.display = 'block';
        generateTable([]);
        document.getElementById('chartWrapper').style.display = 'none';
      }
    } else if (btn.textContent.includes("Compare")) {
      currentVehicle = null;
      generateTable([]);
      updateCompareChart();
      
      // Show chart if any vehicle has results
      const hasResults = Object.values(analyses).some(a => a.results.length > 0);
      document.getElementById('chartWrapper').style.display = hasResults ? 'block' : 'none';
    }
  });
});

// Clear form button
document.querySelectorAll('.btn-secondary').forEach(btn => {
  btn.addEventListener('click', () => {
    if (btn.textContent === 'Clear Form') {
      inputIds.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
          if (id === 'downPayment' || id === 'buyMonthly' || id === 'currentMiles' || id === 'avgTravel') {
            element.value = '0';
          } else {
            element.value = '';
          }
        }
      });
    }
  });
});

// Calculate button
document.getElementById('calculateBtn').addEventListener('click', async function() {
  if (!currentVehicle) {
    alert("Please select a vehicle tab (1, 2, or 3) before calculating.");
    return;
  }

  saveInputs(currentVehicle);
  const currentInputs = analyses[currentVehicle].inputs;

  const apiKey = document.getElementById('apiKey').value;
  
  // Use values from our saved inputs object
  const vin = currentInputs.vin;
  const zip = currentInputs.zip;
  const avgTravel = parseInt(currentInputs.avgTravel, 10) || 0;
  const currentMiles = parseInt(currentInputs.currentMiles, 10) || 0;
  
  const downPayment = parseFloat(currentInputs.downPayment) || 0;
  const buyMonthly = parseFloat(currentInputs.buyMonthly) || 0;
  const buyYears = parseInt(currentInputs.buyTerm, 10);

  if (!vin || !zip || !buyYears || isNaN(buyMonthly)) {
    alert("Please fill in VIN, Zip Code, Term, and Monthly Payment before calculating.");
    return;
  }

  if (!apiKey) {
    alert("Please enter your MarketCheck API Key.");
    return;
  }

  const results = [];

  for (let year = 1; year <= buyYears; year++) {
    const miles = currentMiles + (avgTravel * year);
    const buyPaid = downPayment + (buyMonthly * 12 * year);

    const params = new URLSearchParams({
      api_key: apiKey,
      vin: vin,
      miles: miles,
      dealer_type: 'independent',
      zip: zip,
      is_certified: 'true'
    });

    try {
      const response = await fetch('https://mc-api.marketcheck.com/v2/predict/car/us/marketcheck_price?' + params);
      const data = await response.json();
      const carValue = data.marketcheck_price || 'N/A';

      results.push({ Year: year, Miles: miles, Buy_Paid: buyPaid, Car_Value: carValue });
    } catch (err) {
      results.push({ Year: year, Miles: miles, Buy_Paid: buyPaid, Car_Value: 'Error' });
      console.error('API call error:', err);
    }
  }

  analyses[currentVehicle].results = results;

  generateTable(results);
  updateChart(results, `Vehicle ${currentVehicle}`);
  document.getElementById('chartWrapper').style.display = 'block';
});

// Initialize chart on page load
document.addEventListener('DOMContentLoaded', initChart);