import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { FormBuilder } from '@angular/forms';

import { DataItemDetailsComponent } from './data-item-details.component';


import { providers } from '../../testing/spies';

describe('DataItemDetailsComponent', () => {
  let component: DataItemDetailsComponent;
  let fixture: ComponentFixture<DataItemDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DataItemDetailsComponent ],
      providers: [
        FormBuilder,
        ...providers
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DataItemDetailsComponent);
    component = fixture.componentInstance;
    component.dataItem = { fields: [] };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
