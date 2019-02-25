import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { environment } from '../../environments/environment';

const SCHEMA = [
  {
    name: 'University Degree',
    path: 'university-degrees',
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
    path: 'mexican-identifications',
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

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

const containerId = 'container';

@Injectable()
export class DataItemsService {

  private baseUrl = environment.api.url;

  constructor(private http: HttpClient) { }

  getSchema(): Observable<any[]> {
    // TODO: Retrieve the schema from the server
    return of(SCHEMA);
  }

  create(path: string, id: string, item: any): Observable<any> {
    const url = `${this.baseUrl}/${path}`;
    // TODO: fix me
    const owner = {
      key: '',
      signature: ''
    };
    const body = {
      containerId: containerId,
      content: {
        id: id,
        owners: [owner],
        witnesses: [],
        data: item
      },
      destinationDescriptor: {
        type: 'everyone',
        obj: {}
      }
    };
    return this.http.post(url, body, httpOptions);
  }
}
