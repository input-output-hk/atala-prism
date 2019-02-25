import { Component, OnInit } from '@angular/core';

// TODO: Retrieve schema from the server
const SCHEMA = [
  {
    name: 'University Degree',
    url: 'university-degrees',
    fields: [
      {
        id: 'university',
        name: 'University name',
        type: 'string',
        mandatory: true
      },
      {
        id: 'name',
        name: 'Full name',
        type: 'string',
        mandatory: true
      },
      {
        id: 'graduationYear',
        name: 'Graduation year',
        type: 'integer',
        mandatory: true
      }
    ]
  },
  {
    name: 'Mexican Id',
    url: 'mexican-identifications',
    fields: [
      {
        id: 'name',
        name: 'Full name',
        type: 'string',
        mandatory: true
      },
      {
        id: 'address',
        name: 'Address',
        type: 'string'
      },
      {
        id: 'gender',
        name: 'Gender',
        type: 'string',
        mandatory: true
      },
      {
        id: 'expirationYear',
        name: 'Expiration year',
        type: 'integer',
        mandatory: true
      }
    ]
  }
];

@Component({
  selector: 'app-data-items',
  templateUrl: './data-items.component.html',
  styleUrls: ['./data-items.component.css']
})
export class DataItemsComponent implements OnInit {

  schema = SCHEMA;
  selectedItem = null;

  constructor() { }

  ngOnInit() {
  }

  select(item) {
    this.selectedItem = item;
  }

  isSelected(item): boolean {
    return this.selectedItem === item;
  }
}
