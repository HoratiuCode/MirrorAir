#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../lib/dnssd.h"
#include "../lib/dnssdint.h"

typedef struct dnssd_s {
    char *name;
    int name_len;
    char *hw_addr;
    int hw_addr_len;
    char *airplay_txt;
    int airplay_txt_len;
    unsigned short raop_port;
    unsigned short airplay_port;
    bool raop_registered;
    bool airplay_registered;
} dnssd_t;

static char *dup_bytes(const char *value, int length) {
    char *copy = calloc(1, (size_t) length + 1);
    if (copy == NULL) {
        return NULL;
    }
    memcpy(copy, value, (size_t) length);
    copy[length] = '\0';
    return copy;
}

static char *build_airplay_txt(int *length) {
    const char *format = "deviceid=%s"
                         "\nfeatures=%s"
                         "\nflags=%s"
                         "\nmodel=AppleTV3,2"
                         "\npk=%s"
                         "\npi=%s"
                         "\nsrcvers=%s"
                         "\nvv=%s";
    const char *device_id = "00:00:00:00:00:00";
    const int required = snprintf(
        NULL,
        0,
        format,
        device_id,
        AIRPLAY_FEATURES,
        AIRPLAY_FLAGS,
        AIRPLAY_PK,
        AIRPLAY_PI,
        AIRPLAY_SRCVERS,
        AIRPLAY_VV);
    if (required <= 0) {
        return NULL;
    }

    char *buffer = calloc(1, (size_t) required + 1);
    if (buffer == NULL) {
        return NULL;
    }

    snprintf(
        buffer,
        (size_t) required + 1,
        format,
        device_id,
        AIRPLAY_FEATURES,
        AIRPLAY_FLAGS,
        AIRPLAY_PK,
        AIRPLAY_PI,
        AIRPLAY_SRCVERS,
        AIRPLAY_VV);
    *length = required;
    return buffer;
}

dnssd_t *dnssd_init(const char *name, int name_len, const char *hw_addr, int hw_addr_len, int *error) {
    if (error != NULL) {
        *error = DNSSD_ERROR_NOERROR;
    }

    dnssd_t *dnssd = calloc(1, sizeof(dnssd_t));
    if (dnssd == NULL) {
        if (error != NULL) {
            *error = DNSSD_ERROR_OUTOFMEM;
        }
        return NULL;
    }

    dnssd->name = dup_bytes(name, name_len);
    dnssd->hw_addr = dup_bytes(hw_addr, hw_addr_len);
    dnssd->name_len = name_len;
    dnssd->hw_addr_len = hw_addr_len;
    dnssd->airplay_txt = build_airplay_txt(&dnssd->airplay_txt_len);

    if (dnssd->name == NULL || dnssd->hw_addr == NULL || dnssd->airplay_txt == NULL) {
        dnssd_destroy(dnssd);
        if (error != NULL) {
            *error = DNSSD_ERROR_OUTOFMEM;
        }
        return NULL;
    }

    return dnssd;
}

int dnssd_register_raop(dnssd_t *dnssd, unsigned short port) {
    if (dnssd == NULL) {
        return -1;
    }
    dnssd->raop_port = port;
    dnssd->raop_registered = true;
    return 0;
}

int dnssd_register_airplay(dnssd_t *dnssd, unsigned short port) {
    if (dnssd == NULL) {
        return -1;
    }
    dnssd->airplay_port = port;
    dnssd->airplay_registered = true;
    return 0;
}

void dnssd_unregister_raop(dnssd_t *dnssd) {
    if (dnssd == NULL) {
        return;
    }
    dnssd->raop_registered = false;
    dnssd->raop_port = 0;
}

void dnssd_unregister_airplay(dnssd_t *dnssd) {
    if (dnssd == NULL) {
        return;
    }
    dnssd->airplay_registered = false;
    dnssd->airplay_port = 0;
}

const char *dnssd_get_airplay_txt(dnssd_t *dnssd, int *length) {
    if (dnssd == NULL) {
        return NULL;
    }
    if (length != NULL) {
        *length = dnssd->airplay_txt_len;
    }
    return dnssd->airplay_txt;
}

const char *dnssd_get_name(dnssd_t *dnssd, int *length) {
    if (dnssd == NULL) {
        return NULL;
    }
    if (length != NULL) {
        *length = dnssd->name_len;
    }
    return dnssd->name;
}

const char *dnssd_get_hw_addr(dnssd_t *dnssd, int *length) {
    if (dnssd == NULL) {
        return NULL;
    }
    if (length != NULL) {
        *length = dnssd->hw_addr_len;
    }
    return dnssd->hw_addr;
}

void dnssd_destroy(dnssd_t *dnssd) {
    if (dnssd == NULL) {
        return;
    }
    free(dnssd->name);
    free(dnssd->hw_addr);
    free(dnssd->airplay_txt);
    free(dnssd);
}
