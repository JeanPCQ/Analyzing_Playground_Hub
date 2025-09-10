// A more robust data structure to hold both inputs and results for each analysis
const analyses = {
  1: {
    inputs: { vin: '', zip: '', buyTerm: '', downPayment: '0', buyMonthly: '0' },
    results: []
  },
  2: {
    inputs: { vin: '', zip: '', buyTerm: '', downPayment: '0', buyMonthly: '0' },
    results: []
  },
  3: {
    inputs: { vin: '', zip: '', buyTerm: '', downPayment: '0', buyMonthly: '0' },
    results: []
  }
};

let currentVehicle = 1; // Default active vehicle

// Map input IDs to keys in our data structure for easy saving and loading
const inputIds = ['vin', 'zip', 'buyTerm', 'downPayment', 'buyMonthly'];

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

// Chart.js setup (no changes here)
const ctx = document.getElementById('carChart').getContext('2d');
const carChart = new Chart(ctx, {
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

// Generate table (no changes here)
function generateTable(data) {
  const resultsDiv = document.getElementById('results');
  resultsDiv.innerHTML = '';
  if (data.length === 0) return;

  const table = document.createElement('table');
  table.style.borderCollapse = 'collapse';
  table.style.width = '100%';

  // Header
  const thead = document.createElement('thead');
  const headerRow = document.createElement('tr');
  ['Year', 'Miles', 'Investment', 'Car Value'].forEach(text => {
    const th = document.createElement('th');
    th.textContent = text;
    th.style.border = '1px solid #ccc';
    th.style.padding = '5px';
    headerRow.appendChild(th);
  });
  thead.appendChild(headerRow);
  table.appendChild(thead);

  // Body
  const tbody = document.createElement('tbody');
  data.forEach(row => {
    const tr = document.createElement('tr');
    [row.Year, row.Miles, row.Buy_Paid ?? 'N/A', row.Car_Value].forEach(cellData => {
      const td = document.createElement('td');
      td.textContent = cellData;
      td.style.border = '1px solid #ccc';
      td.style.padding = '5px';
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  resultsDiv.appendChild(table);
}

// Update chart for single vehicle (no changes here)
function updateChart(data, label) {
  carChart.data.labels = data.map(d => `Year ${d.Year}`);
  carChart.data.datasets = [
    {
      label: `${label} Car Value`,
      data: data.map(d => d.Car_Value === 'N/A' || d.Car_Value === 'Error' ? null : Number(d.Car_Value)),
      borderColor: 'blue',
      fill: false,
      tension: 0.2
    },
    {
      label: `${label} Investment`,
      data: data.map(d => d.Buy_Paid ?? null),
      borderColor: 'red',
      fill: false,
      tension: 0.2
    }
  ];
  carChart.update();
}

// Update chart for all vehicles
function updateCompareChart() {
  carChart.data.datasets = [];
  carChart.data.labels = [];

  const colors = ['#007bff', '#28a745', '#6f42c1']; // Blue, Green, Purple

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
        borderColor: colors[idx],
        fill: false,
        tension: 0.2
      });

      // Add the "Investment" dataset for this vehicle (Dashed Line)
      carChart.data.datasets.push({
        label: `Vehicle ${key} Investment`,
        data: data.map(d => d.Buy_Paid ?? null),
        borderColor: colors[idx], // Same color as the value line
        borderDash: [5, 5],      // Makes the line dashed
        fill: false,
        tension: 0.2
      });
    }
  });

  carChart.update();
}

// Tab switching
document.querySelectorAll('.tab').forEach((btn, idx) => {
  btn.addEventListener('click', () => {
    // **MODIFIED** to save current inputs before switching
    saveInputs(currentVehicle);

    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (btn.textContent.includes("Vehicle")) {
      currentVehicle = idx + 1;
      loadInputs(currentVehicle); // Load the inputs for the new vehicle

      const currentAnalysis = analyses[currentVehicle];
      if (currentAnalysis.results.length > 0) {
        generateTable(currentAnalysis.results);
        updateChart(currentAnalysis.results, `Vehicle ${currentVehicle}`);
      } else {
        document.getElementById('results').innerHTML = "No data yet. Enter details and calculate.";
        generateTable([]);
        updateChart([], ''); // Clear chart
      }
    } else if (btn.textContent.includes("Compare")) {
      currentVehicle = null; // No single vehicle is active
      generateTable([]); // Clear table
      updateCompareChart();
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
  const avgTravel = document.getElementById('avgTravel').value;
  const currentMiles = document.getElementById('currentMiles').value;

  // Use values from our saved inputs object
  const vin = currentInputs.vin;
  const zip = currentInputs.zip;
  
  // **FIX 1: Parse all values and provide a fallback of 0 to prevent NaN**
  const downPayment = parseFloat(currentInputs.downPayment) || 0;
  const buyMonthly = parseFloat(currentInputs.buyMonthly) || 0;
  const buyYears = parseInt(currentInputs.buyTerm, 10);

  if (!vin || !zip || !buyYears || isNaN(buyMonthly)) {
    alert("Please fill in VIN, Zip Code, Term, and Monthly Payment before calculating.");
    return;
  }

  const results = [];

  for (let year = 1; year <= buyYears; year++) {
    const miles = currentMiles + avgTravel * year;
    
    // **FIX 2: Updated formula to include the down payment in the cumulative total**
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
});