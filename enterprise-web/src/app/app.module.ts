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
import { ErrorService } from './services/error.service';
import { NotificationService } from './services/notification.service';
import { IdentityRepository } from './services/identity.repository';

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    FooterComponent,
    HomeComponent,
    IdentityLedgerComponent
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
    KeyPairsService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
