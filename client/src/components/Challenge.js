import { useState } from 'react';

export default function Challenge() {

  // to insert the cost value to the total cost
  const [costText, changeCost] = useState('0');
  
  // to insert json data to the table
  const [data, setItemRow] = useState();
  
  // bool value to show the table only when the button is pressed
  const [showTable, setShowTable] = useState(false);
  
  // for input field
  //const [orderQuantity, setOrderQuantity] = useState('0');
  
  var orderItems = []
  
  const setOrderQuantity = (value, ID, eState) =>{
  	console.log("Reorder quantity: "+value+" Reorder ID: "+ID);
  	//orderItems.push({ID, value});
  	// update array if ID exists, avoid duplicates
  	const idx = orderItems.findIndex(a => a.ID === ID);
  	
  	if(idx === -1){
  	  orderItems.push({ID, value});
  	}
  	else{
  	  orderItems[idx].value = value; 
  	}
  	
  	console.log(orderItems);
  }
    
  // low-stock items button handler
  const orderCost = () => {
  	console.log('Button order cost working')
  	
  	fetch('http://localhost:4567/restock-cost', {
  	  method: 'POST',
  	  headers: {"Content-Type" : "application/json"},
  	  body: JSON.stringify(orderItems)
  	}).then(response =>{
  	    //console.log(response)
  	    return response.json();
  	}).then(j =>{
  	    //console.log(j[0]["cost"])
  	    changeCost(j[0]["cost"])
  	})
  	orderItems = []
  	document.querySelectorAll('input').forEach(input => (input.value = ""));
  }
  
  // determine re-order cost button handler
  const lowStockButton = () => {
  	console.log('lowStockButton working')
  	fetch('http://localhost:4567/low-stock')
  		.then(res => {
  			return res.json();  		
  		})
  		.then(data => {
  			console.log(data);
  			setItemRow(data);
  			setShowTable(true);
  		})
  }
  

  return (
    <>
      <table>
        <thead>
          <tr>
            <td>SKU</td>
            <td>Item Name</td>
            <td>Amount in Stock</td>
            <td>Capacity</td>
            <td>Order Amount</td>
          </tr>
        </thead>
        <tbody>
          {/* 
          TODO: Create an <ItemRow /> component that's rendered for every inventory item. The component
          will need an input element in the Order Amount column that will take in the order amount and 
          update the application state appropriately.
          */
          
          showTable && data.map((row)=> (
          	<tr>
          	  <td>{row.ID}</td>
          	  <td>{row.Name}</td>
          	  <td>{row.Stock}</td>
          	  <td>{row.Capacity}</td>
          	  <td> 
          	    <input 
          	      type="text" 
          	      id="inp"
          	      placeholder="0"
          	      //value={orderQuantity}
          	      onChange={(e)=> setOrderQuantity(e.target.value, row.ID)}
          	    />
          	  </td>
          	</tr>
          ))
          
          }
          
        </tbody>
      </table>
      {/* TODO: Display total cost returned from the server */}
      <div>Total Cost: 
      	<p id="cost">{costText}</p>
      </div>
      {/* 
      TODO: Add event handlers to these buttons that use the Java API to perform their relative actions.
      */
  
      }
      
      <button onClick={lowStockButton}>Get Low-Stock Items</button>
      <button onClick={orderCost}>Determine Re-Order Cost</button>      

      
    </>  
    
  );
}
