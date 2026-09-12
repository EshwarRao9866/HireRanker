import { Pipe, PipeTransform } from '@angular/core';
import { formatSalaryToLpa } from '../services/salary-formatter.util';

@Pipe({
  name: 'salary',
  standalone: true
})
export class SalaryPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    return formatSalaryToLpa(value);
  }
}
