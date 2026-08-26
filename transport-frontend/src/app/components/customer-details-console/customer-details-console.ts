import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CustomerMgmtService, CustomerContact, CustomerDeliverySite, CustomerDocument } from '../../services/customer-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfTextareaComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-customer-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfTextareaComponent,
    FfButtonComponent
  ],
  templateUrl: './customer-details-console.html',
  styleUrl: './customer-details-console.css'
})
export class CustomerDetailsConsoleComponent implements OnInit {
  private customerMgmtService = inject(CustomerMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  activeTab = signal<string>('sites');
  customerId = signal<number | null>(null);
  customers = signal<any[]>([]);
  loading = signal<boolean>(false);

  readonly selectedCustomer = computed(() => {
    const id = this.customerId();
    return this.customers().find(c => c.id === id) || null;
  });

  readonly selectedCustomerLabel = computed(() => {
    const c = this.selectedCustomer();
    if (!c) return 'No customer selected';
    return [c.code, c.name].filter(Boolean).join(' — ');
  });

  contacts = signal<CustomerContact[]>([]);
  deliverySites = signal<CustomerDeliverySite[]>([]);
  documents = signal<CustomerDocument[]>([]);
  documentTypeOptions: FfSelectOption[] = [
    { label: 'GST CERTIFICATE', value: 'GST_CERT' },
    { label: 'PAN CARD', value: 'PAN_CARD' },
    { label: 'KYC REGISTRATION', value: 'KYC' },
    { label: 'LEGAL AGREEMENT', value: 'AGREEMENT' }
  ];

  get customerOptions(): FfSelectOption[] {
    return [
      { label: '-- Select Customer --', value: '' },
      ...this.customers().map(c => ({
        label: `${c.code || ''} ${c.name || ''}`.trim(),
        value: c.id
      }))
    ];
  }

  customerSelectForm!: FormGroup;
  contactForm!: FormGroup;
  siteForm!: FormGroup;
  documentForm!: FormGroup;

  showDocEditor = signal<boolean>(false);
  showContactEditor = signal<boolean>(false);
  showSiteEditor = signal<boolean>(false);

  ngOnInit() {
    this.initForms();
    this.loadCustomers();
  }

  loadCustomers() {
    this.masterService.getMasters<any>('customers', this.companyId, { size: 100, page: 0 }).subscribe(res => {
      if (res.success && res.data) {
        const list = res.data.content || res.data || [];
        this.customers.set(list);
        if (list.length && this.customerId() == null) {
          this.selectCustomer(list[0].id);
        }
      }
    });
  }

  selectCustomer(id: number | string | null) {
    const customerId = id === '' || id == null ? null : Number(id);
    this.customerId.set(customerId);
    this.customerSelectForm.patchValue({ customerId: customerId ?? '' }, { emitEvent: false });
    this.contacts.set([]);
    this.deliverySites.set([]);
    this.documents.set([]);
    if (customerId == null) return;
    this.loadContacts();
    this.loadSites();
    this.loadDocuments();
  }

  initForms() {
    this.customerSelectForm = this.fb.group({
      customerId: ['']
    });

    this.customerSelectForm.get('customerId')!.valueChanges.subscribe(value => {
      this.selectCustomer(value);
    });

    this.contactForm = this.fb.group({
      contactName: ['', [Validators.required, Validators.maxLength(150)]],
      designation: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.maxLength(20)]]
    });

    this.siteForm = this.fb.group({
      siteCode: ['', [Validators.required, Validators.maxLength(50)]],
      siteName: ['', [Validators.required, Validators.maxLength(150)]],
      address: ['', Validators.required],
      managerName: ['']
    });

    this.documentForm = this.fb.group({
      docType: ['GST_CERT', Validators.required],
      docNumber: ['', [Validators.required, Validators.maxLength(50)]],
      filePath: ['']
    });
  }

  openSiteEditor() {
    if (!this.customerId()) return;
    this.showSiteEditor.set(true);
  }

  openContactEditor() {
    if (!this.customerId()) return;
    this.showContactEditor.set(true);
  }

  openDocEditor() {
    if (!this.customerId()) return;
    this.showDocEditor.set(true);
  }

  loadContacts() {
    const id = this.customerId();
    if (id == null) return;
    this.customerMgmtService.getContacts(id).subscribe(res => {
      if (res.success && res.data) {
        this.contacts.set(res.data);
      }
    });
  }

  loadSites() {
    const id = this.customerId();
    if (id == null) return;
    this.customerMgmtService.getSites(id).subscribe(res => {
      if (res.success && res.data) {
        this.deliverySites.set(res.data);
      }
    });
  }

  loadDocuments() {
    const id = this.customerId();
    if (id == null) return;
    this.customerMgmtService.getDocuments(id).subscribe(res => {
      if (res.success && res.data) {
        this.documents.set(res.data);
      }
    });
  }

  saveContact() {
    const id = this.customerId();
    if (id == null || this.contactForm.invalid) return;

    this.loading.set(true);
    this.customerMgmtService.addContact(id, this.contactForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Contact person saved successfully');
        this.loadContacts();
        this.showContactEditor.set(false);
        this.contactForm.reset();
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save contact person');
      }
    });
  }

  deleteContact(contact: CustomerContact) {
    const id = this.customerId();
    if (!contact.id || id == null) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Contact Person',
        message: `Are you sure you want to remove contact: ${contact.contactName}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && contact.id) {
        this.customerMgmtService.deleteContact(id, contact.id).subscribe({
          next: () => {
            this.notify.success('Contact person removed successfully');
            this.loadContacts();
          },
          error: () => this.notify.error('Failed to remove contact person')
        });
      }
    });
  }

  saveSite() {
    const id = this.customerId();
    if (id == null || this.siteForm.invalid) return;

    this.loading.set(true);
    this.customerMgmtService.addSite(id, this.siteForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Delivery site saved successfully');
        this.loadSites();
        this.showSiteEditor.set(false);
        this.siteForm.reset();
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save delivery site');
      }
    });
  }

  deleteSite(site: CustomerDeliverySite) {
    const id = this.customerId();
    if (!site.id || id == null) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Delivery Site',
        message: `Are you sure you want to delete unloading site: ${site.siteName}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && site.id) {
        this.customerMgmtService.deleteSite(id, site.id).subscribe({
          next: () => {
            this.notify.success('Delivery site deleted successfully');
            this.loadSites();
          },
          error: () => this.notify.error('Failed to delete delivery site')
        });
      }
    });
  }

  saveDocument() {
    const id = this.customerId();
    if (id == null || this.documentForm.invalid) return;

    this.loading.set(true);
    this.customerMgmtService.addDocument(id, this.documentForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Document saved successfully');
        this.loadDocuments();
        this.showDocEditor.set(false);
        this.documentForm.reset({ docType: 'GST_CERT' });
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save document');
      }
    });
  }

  deleteDocument(doc: CustomerDocument) {
    const id = this.customerId();
    if (!doc.id || id == null) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Document',
        message: `Are you sure you want to remove document registration: ${doc.docNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && doc.id) {
        this.customerMgmtService.deleteDocument(id, doc.id).subscribe({
          next: () => {
            this.notify.success('Document deleted successfully');
            this.loadDocuments();
          },
          error: () => this.notify.error('Failed to delete document')
        });
      }
    });
  }
}
