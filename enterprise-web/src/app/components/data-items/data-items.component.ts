import { Component, OnInit } from '@angular/core';

import { DataItemsService } from '../../services/data-items.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-data-items',
  templateUrl: './data-items.component.html',
  styleUrls: ['./data-items.component.css']
})
export class DataItemsComponent implements OnInit {

  schema$: Observable<any[]>;
  selectedItem = null;

  constructor(private dataItems: DataItemsService) { }

  ngOnInit() {
    this.schema$ = this.dataItems.getSchema();
  }

  select(item) {
    this.selectedItem = item;
  }

  isSelected(item): boolean {
    return this.selectedItem === item;
  }
}
