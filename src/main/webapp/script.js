import {Interactive} from "https://vectorjs.org/index.js";

// Construct an interactive within the HTML element with the id "my-interactive"

let cSize = 800;
let myInteractive = new Interactive("my-interactive",{
	width: cSize,
	height: cSize,
	originX: 0,
	originY: 0
});
myInteractive.border = true;

// Set up some constants
const config = { 
	"colors": {
		"board": {
			"black": "#997052",
			"white": "#E9C385"	
		},
		"pieces": {
			"black": "#3f322e",
			"red": "#E35859"
		}
	}	
};
let cAdj = cSize / 10;

// Construct the board
for(let i = 0; i < 64; i++) {
	let xOff = i % 8;
	let yOff = Math.floor(i / 8);
	let rect = myInteractive.rectangle(xOff * cAdj + cAdj, yOff * cAdj + cAdj, cAdj, cAdj);
	if(((i % 8) % 2) == (Math.floor(i / 8) % 2)) rect.style.fill = config.colors.board.white;
	else rect.style.fill = config.colors.board.black;	
}

// Construct some pieces
let cHalf = Math.floor(cAdj / 2);
let cRad = Math.floor(cAdj * 0.85 * 0.5);
for(let i = 0; i < 64; i++) {
	let xOff = i % 8;
	let yOff = Math.floor(i / 8);
	if(yOff < 3) {
		if(xOff % 2 == (yOff + 1) % 2) {
			let bpiece = myInteractive.ellipse(xOff * cAdj + cAdj + cHalf, yOff * cAdj + cAdj + cHalf, cRad, cRad);
			bpiece.style.fill = config.colors.pieces.black;
		}
	}
	if(yOff > 4) {
		if(xOff % 2 == (yOff + 1) % 2) {
			let rpiece = myInteractive.ellipse(xOff * cAdj + cAdj + cHalf, yOff * cAdj + cAdj + cHalf, cRad, cRad);
			rpiece.style.fill = config.colors.pieces.red;
		}
	}
}
// Print the two objects to the console
console.log( control, myInteractive);
