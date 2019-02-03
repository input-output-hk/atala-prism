import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class KeyPairsService {

  private baseUrl = environment.api.url + '/signing-key-pairs';

  constructor(private http: HttpClient) { }

  generate(): Observable<any> {
    const url = `${this.baseUrl}`;
    const body = {};
    return this.http.post(url, body, httpOptions);
  }
}
