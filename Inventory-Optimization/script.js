//Step 1: Creating a Sampel Data

const inventorysample: [
    {Location: "NJ", Item: "Item A", Safety_Stock: 3, Inventory: 2000, Months_Sold: 600, Months_Received: 2000},
    {Location: "NY", Item: "Item A", Safety_Stock: 3, Inventory: 500, Months_Sold: 2000, Months_Received: 3000},
    {Location: "PA", Item: "Item A", Safety_Stock: 3, Inventory: 200, Months_Sold: 200, Months_Received: 200},
    {Location: "TX", Item: "Item A", Safety_Stock: 3, Inventory: 1000, Months_Sold: 4000, Months_Received: 6000}
];

// default value
let defaultMonths = 12;

//User Enters Values
function useNumber() {
  let num = document.getElementById("userNumber").value;
  num = parseInt(num, 10);

  if (isNaN(num)) {
    document.getElementById("numberOutput").innerText = "⚠️ Please enter a valid whole number.";
  } else {
    document.getElementById("numberOutput").innerText = "You entered: " + num;
    console.log("Threshold entered:", num);
  }
}

//MOS (Month of Supply)
function MOS(data) {
    return data.map(record => {
        let mos = record.Inventory / (record.Months_Sold / defaultMonths);
        return {
            ...record, //Keep all existing record
            MOS: mos // add new column
        };
    });
}