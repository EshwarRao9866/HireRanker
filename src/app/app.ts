// import { Component, signal } from '@angular/core';
// // import { HomePage } from './home-page/home-page';
// import { FormsModule } from '@angular/forms';
// import { RouterOutlet } from '@angular/router';
// import { Register } from './register/register';

// @Component({
//   selector: 'app-root',
//   imports: [ FormsModule, RouterOutlet, Register],
//   templateUrl: './app.html',
//   styleUrl: './app.css'
// })
// export class App {

// }
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

}