import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { FooterComponent } from './components/footer/footer.component';
import { HomeComponent } from './components/home/home.component';
import { IdentityLedgerComponent } from './components/identity-ledger/identity-ledger.component';

import { IdentityLedgerService } from './services/identity-ledger.service';
import { KeyPairsService } from './services/key-pairs.service';
import { DataItemsService } from './services/data-items.service';
import { ErrorService } from './services/error.service';
import { NotificationService } from './services/notification.service';
import { IdentityRepository } from './services/identity.repository';
import { IdentityDetailsComponent } from './components/identity-details/identity-details.component';
import { DataItemsComponent } from './components/data-items/data-items.component';
import { DataItemDetailsComponent } from './components/data-item-details/data-item-details.component';

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    FooterComponent,
    HomeComponent,
    IdentityLedgerComponent,
    IdentityDetailsComponent,
    DataItemsComponent,
    DataItemDetailsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    BrowserAnimationsModule
  ],
  providers: [
    NotificationService,
    ErrorService,
    IdentityRepository,
    IdentityLedgerService,
    KeyPairsService,
    DataItemsService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
