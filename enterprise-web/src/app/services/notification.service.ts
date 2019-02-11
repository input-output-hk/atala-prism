import { Injectable } from '@angular/core';

@Injectable()
export class NotificationService {

  // TODO: Add support for pretty notifications, like toastr
  constructor() { }

  info(message: string) {
    alert(message);
  }

  error(message: string) {
    alert(message);
  }
}
