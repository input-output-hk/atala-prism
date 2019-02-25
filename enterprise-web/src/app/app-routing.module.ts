import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './components/home/home.component';
import { IdentityLedgerComponent } from './components/identity-ledger/identity-ledger.component';
import { DataItemsComponent } from './components/data-items/data-items.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'identity-ledger', component: IdentityLedgerComponent },
  { path: 'data-items', component: DataItemsComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
