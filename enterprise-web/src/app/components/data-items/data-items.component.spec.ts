import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { DataItemsComponent } from './data-items.component';
import { providers } from '../../testing/spies';

describe('DataItemsComponent', () => {
  let component: DataItemsComponent;
  let fixture: ComponentFixture<DataItemsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DataItemsComponent ],
      providers: providers,
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DataItemsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
