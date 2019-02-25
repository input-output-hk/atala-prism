import { Component, OnInit, Input } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { ErrorService } from '../../services/error.service';
import { DataItemsService } from '../../services/data-items.service';

@Component({
  selector: 'app-data-item-details',
  templateUrl: './data-item-details.component.html',
  styleUrls: ['./data-item-details.component.css']
})
export class DataItemDetailsComponent implements OnInit {

  _dataItem: any;
  form: FormGroup;

  constructor(
    public errorService: ErrorService,
    private formBuilder: FormBuilder,
    private dataItems: DataItemsService) { }

  ngOnInit() {
  }

  @Input()
  set dataItem(_dataItem: any) {
    this._dataItem = _dataItem;
    this.createForm();
  }

  get dataItem(): any {
    return this._dataItem;
  }

  private createForm() {
    const values = {};
    this._dataItem.fields.forEach(field => {
      const validators = [];
      if (field.mandatory) {
        validators.push(Validators.required);
      }
      if (field.type === 'integer') {
        // TODO: fix RegEx
        validators.push(Validators.pattern('^([0-9]+)$'));
      }
      values[field.id] = [null, validators];
    });

    this.form = this.formBuilder.group(values);
  }

  getHtmlType(field): string {
    if (field.type === 'string') {
      return 'text';
    } else if (field.type === 'integer') {
      return 'number';
    } else {
      return 'text';
    }
  }

  create() {
    const item = {};
    this._dataItem.fields.forEach(field => {
      item[field.id] = this.form.controls[field.id].value;
    });

    const id = 'item-id' // TODO: how to generate it?
    console.log('trying to create item');
    console.log(item);
    this.dataItems.create(this._dataItem.path, id, item).subscribe(response => console.log(response));
  }
}
